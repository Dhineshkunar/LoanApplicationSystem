# Interview Guide — Loan Approval System

This document is the "explain it like an interviewer expects" companion
to the code. Each section: why the piece exists, why production projects
use it this way, common interview questions, and best practices.

---

## 1. Layered architecture

**Why it exists:** Controller → Service → Repository is the standard
Spring Boot layering. Each layer has one job:
- **Controller** — HTTP concerns only (status codes, request/response
  shape, path/query binding).
- **Service** — business rules, transactions, orchestration.
- **Repository** — persistence only.

**Why production projects use it:** it isolates change. A DB migration
only touches the repository layer; a new validation rule only touches
DTOs/service; an API contract change only touches controller/DTO. It also
makes each layer independently unit-testable (mock the layer below).

**Common interviewer questions:**
- *"Why not put JPA queries directly in the controller?"* — Because it
  couples HTTP handling to persistence, makes the logic untestable
  without a servlet container, and breaks single responsibility.
- *"What layer owns @Transactional?"* — Service (see `LoanServiceImpl`
  javadoc — a full explanation is there).

**Best practice:** dependencies point one direction only —
controller → service → repository. Never the reverse.

---

## 2. `enums/LoanStatus.java` and `enums/LoanType.java`

**Why it exists:** the loan status is not just a label — it's a **state
machine**. `LoanStatus` encodes the transition table directly in the
enum via a `nextAllowed()` method overridden per constant, plus the
`canMoveTo()` helper the spec asked for.

**Why production projects use it:** without this, "can a PENDING loan
jump straight to DISBURSED?" is a rule that lives only in someone's head
or scattered `if` statements. Centralizing it in the enum means the rule
is enforced identically everywhere it's checked, and is trivial to unit
test in isolation (`assertFalse(LoanStatus.PENDING.canMoveTo(APPROVED))`).

**Common interviewer questions:**
- *"Why enum and not a database-driven status table?"* — A DB-driven
  table is more flexible (admin can add statuses without a deploy) but
  loses compile-time safety and IDE autocomplete, and the transition
  rules would need their own table + code to interpret, adding real
  complexity for little gain when the status set is small and stable
  (this is a legitimate trade-off to discuss out loud in interview).
- *"Why store as STRING not ORDINAL?"* — see the entity's javadoc:
  ordinal breaks if the enum is ever reordered.
- *"Why abstract method per constant instead of a switch statement?"* —
  Adding a new status forces you to implement `nextAllowed()` for it
  (compiler enforces it) — a `switch` can be forgotten and silently fall
  through to a default case.

**Best practice:** never let two different layers duplicate the same
transition logic (e.g. also checking it in the controller) — one source
of truth.

---

## 3. `entity/LoanApplication.java` + `entity/BaseEntity.java`

**Why it exists:** the JPA-mapped representation of a row in
`loan_applications`.

**Common interviewer questions:**
- *"Why BigDecimal for money, not double?"* — floating point binary
  representation can't exactly represent most decimal fractions (0.1 +
  0.2 != 0.3 in binary floating point), which is unacceptable once real
  currency is involved. BigDecimal with explicit `precision`/`scale` is
  the standard.
- *"Why avoid `@Data` on JPA entities?"* — `@Data` generates
  `equals()/hashCode()` over all fields, which for a JPA entity should
  really be based on the primary key once persisted; it also generates a
  `toString()` that can trigger unwanted lazy-loading of associations,
  and can produce infinite recursion with bidirectional relationships.
  Prefer targeted `@Getter/@Setter`.
- *"What does `@MappedSuperclass` do vs `@Entity`?"* — see
  `BaseEntity.java` javadoc.
- *"How do createdAt/updatedAt get set without service code touching
  them?"* — `@EnableJpaAuditing` (on the main class) + `@CreatedDate` /
  `@LastModifiedDate` + `@EntityListeners(AuditingEntityListener.class)`.

**Best practice:** indexes on columns you filter/sort by frequently
(`status`, `loan_type` here) — declared directly on the entity via
`@Table(indexes = ...)` so the schema intent lives next to the mapping.

---

## 4. DTOs (`LoanRequestDTO`, `LoanResponseDTO`, `StatusUpdateDTO`)

**Why they exist:** the API contract must never be the same object as
the persistence model. See `LoanRequestDTO`'s javadoc for the full
reasoning (mainly: prevents clients from setting fields like `status` or
`id` directly, and decouples schema evolution from API evolution).

**Common interviewer questions:**
- *"Why not just add `@JsonIgnore` on the entity's status field for
  input and expose it for output?"* — that only solves the serialization
  half of the problem. The entity would still be the JPA-managed object
  bound directly by `@RequestBody`, meaning Hibernate might try to
  attach/merge whatever the client sent, including relationships or
  stale versions — a well-known source of "over-posting" vulnerabilities.
- *"Why three DTOs instead of one shared DTO?"* — `LoanRequestDTO` (input
  to create/update), `LoanResponseDTO` (output), `StatusUpdateDTO`
  (narrow, single-purpose input) each represent a different contract.
  Reusing one bloated DTO for all three means validation annotations that
  don't apply to every use bleed across endpoints.

**Best practice:** validation annotations live on the DTO, never on the
entity — the entity may legitimately hold data that wouldn't pass
"creation" validation once loaded from the DB in a later state.

---

## 4b. `dto/ApiResponse.java` — the success envelope

**Why it exists:** before this class, a 404 returned
`{ "status", "error", "message", "path" }` (from `ErrorResponse`) but a
200 returned the bare DTO with no top-level `status`/`message` at all —
two different shapes for the same endpoint depending on outcome.
`ApiResponse<T>` gives every 2xx response the same predictable envelope:
`timestamp`, `status`, `message`, `data` — the deliberate success
counterpart to `ErrorResponse`.

**Why it's a separate class from `ErrorResponse`, not one shared type**
with nullable `data`/`error` fields: success and failure carry
genuinely different data (a payload vs. an error code + validation map).
Cramming both into one class means most fields are null depending on the
branch — exactly the "nullable soup" shape production teams avoid. Two
small, fully-populated classes are easier for both backend and frontend
to reason about.

**Why `DELETE` is the one endpoint that stays unwrapped:** `204 No
Content` is defined by the HTTP spec as carrying no body at all. Wrapping
it in `ApiResponse` would mean either violating that contract or sending
an empty-but-present shell that just confuses a client correctly
expecting nothing.

**Common interviewer questions:**
- *"Doesn't wrapping data reduce REST purity (HATEOAS-style resource
  representations)?"* — strictly, yes; a purist REST response IS the
  resource, with no envelope. In practice, most production APIs
  (Stripe, GitHub, etc.) trade a little purity for a consistent
  `status`/`message`/`data`/`meta` envelope because it makes client-side
  parsing and error handling uniform across every endpoint — a
  pragmatic, widely-accepted trade-off worth naming explicitly if asked.
- *"Why include `status` inside the JSON body when it's already on the
  HTTP status line?"* — the HTTP status line isn't always reliably
  accessible to every consumer (some logging pipelines, some legacy
  SOAP-style clients, some webhook relays only capture the body), and
  having it in-body means the payload is self-describing even out of
  HTTP context. It's redundant by design, not by accident.
- *"Should the frontend read `response.status` (HTTP) or
  `body.data.status`(business `status` for loan) or `body.status`
  (envelope)?"* — three different things, worth being precise about:
  HTTP status drives protocol-level handling (retry, redirect-to-login),
  the envelope's `status`/`message` drive UI toast/banner text, and the
  loan's own business `status` field (`PENDING`, `APPROVED`, ...) is
  domain data, not a response code at all.

---

## 5. `mapper/LoanMapper.java` (MapStruct)

Full "why MapStruct" reasoning is in the class javadoc; summary for quick
recall in an interview:

1. **Compile-time, not reflection** — generates a real `LoanMapperImpl`
   class at build time; no runtime reflection cost, and a broken mapping
   is a compile error, not a production NPE.
2. **Less boilerplate than hand-written mappers**, while still being 100%
   readable, generated Java you can open and debug.
3. **Refactor-safe** — rename an entity field and the generated mapper
   fails to compile immediately, instead of silently mapping `null`.
4. **`componentModel = "spring"`** — the generated impl is a Spring bean,
   injected into the service like anything else.

**Common interviewer questions:**
- *"MapStruct vs ModelMapper vs manual mapping — which would you pick and
  why?"* — Manual mapping for one or two tiny DTOs is fine and adds zero
  dependencies; MapStruct wins once you have more than a handful of
  DTOs/entities or need it enforced consistently across a team, because
  the compile-time safety scales far better than either alternative.
  ModelMapper is reflection-based like the problem MapStruct solves —
  rarely the right choice on a new project today.
- *"How does `@MappingTarget` work?"* — tells MapStruct to mutate an
  existing object in place (used in `updateEntityFromDto`) instead of
  constructing + returning a new one, which matters for a JPA-managed
  entity that must keep its identity/other fields.

---

## 6. `repository/LoanRepository.java`

**Why it exists:** the sole point of contact with the database for this
aggregate. Extends `JpaRepository<LoanApplication, Long>` for CRUD +
pagination for free, plus derived query methods.

**Common interviewer questions:**
- *"How does Spring generate SQL from `findByStatus`?"* — at startup,
  Spring Data parses the method name against the entity metamodel and
  builds the corresponding JPQL/SQL; no method body needed.
- *"When would you switch to `@Query` instead of a derived method
  name?"* — once the method name would get unreadably long, or the query
  needs joins/aggregations that don't map cleanly to name-derivation.
- *"How does pagination avoid loading the whole table?"* — `Pageable`
  gets translated to `LIMIT`/`OFFSET` (or an equivalent keyset strategy)
  at the SQL level by Spring Data / Hibernate — never "fetch all, then
  slice in Java."

---

## 7. `service/LoanService.java` + `service/impl/LoanServiceImpl.java`

Full transactional reasoning is in the impl's javadoc. Key points to say
out loud in an interview:

- `@Transactional` belongs on the **service** layer, since a use case
  (potentially touching multiple repositories) is the natural
  transaction boundary — not the controller (HTTP layer) or repository
  (single-table concern).
- Class-level `@Transactional(readOnly = true)` as the default, with
  write methods overriding it — an easy, explicit way to get read
  optimizations on the common case (GETs) without repeating the
  annotation on every read method.
- Business rules (default status on create, status-transition
  validation) live here, not in the controller or the entity.

**Common interviewer questions:**
- *"What happens if an exception is thrown inside a `@Transactional`
  method?"* — for unchecked exceptions (like our
  `InvalidStatusTransitionException`/`IllegalStateException`), Spring
  rolls back the transaction automatically. Checked exceptions do NOT
  trigger rollback by default (would need
  `@Transactional(rollbackFor = ...)`), which is worth mentioning even
  though this codebase only uses unchecked exceptions.
- *"Why interface + impl for a simple CRUD service?"* — see
  `LoanService.java` javadoc; not always necessary, but standard
  practice for testability and future flexibility.

---

## 8. `controller/LoanController.java`

**Why it exists:** the only class that knows about HTTP. Talks in
`ResponseEntity<T>` so each endpoint controls its own status code
explicitly rather than always defaulting to 200.

**Why `PATCH /loans/{id}/status` is separate from `PUT /loans/{id}`:**
see the controller's javadoc — status transitions are a distinct
operation with their own rule (the state machine) and their own failure
mode (409, not 400).

**Common interviewer questions:**
- *"PUT vs PATCH — what's the actual semantic difference?"* — PUT
  represents a full replacement of a resource's editable representation;
  PATCH represents a partial modification. Here, `PUT /loans/{id}` takes
  the same full `LoanRequestDTO` as create (full replace of editable
  fields), while `PATCH .../status` changes exactly one field — matching
  PATCH's "partial update" semantics precisely.
- *"Why return `Location` header on 201?"* — it's the REST convention
  telling the client exactly where to `GET` the newly created resource;
  many API clients/tools surface this automatically.
- *"Why is there no try/catch in the controller?"* — `GlobalExceptionHandler`
  (`@RestControllerAdvice`) intercepts exceptions thrown anywhere in the
  request-handling chain, so controllers stay focused on the happy path.

---

## 9. Exception handling (`exception/*`)

**Why it exists:** a single, predictable error contract
(`ErrorResponse`) for every failure mode, enforced in one place
(`GlobalExceptionHandler`) instead of duplicated try/catch in every
controller method.

**Mapping table (also the answer to "explain each HTTP code"):**

| Exception | HTTP Status | When it's thrown here |
|---|---|---|
| `ResourceNotFoundException` | **404 Not Found** | `GET/PUT/DELETE /loans/{id}` when the id doesn't exist |
| `MethodArgumentNotValidException` | **400 Bad Request** | `@Valid` fails on `LoanRequestDTO`/`StatusUpdateDTO` (e.g. blank name, bad email) |
| `IllegalStateException` (incl. `InvalidStatusTransitionException`) | **409 Conflict** | `PATCH /loans/{id}/status` requests an illegal transition |
| `Exception` (catch-all) | **500 Internal Server Error** | Any unexpected failure (DB down, NPE bug, etc.) — never leaks the stack trace to the client |

**Common interviewer questions:**
- *"Why does `InvalidStatusTransitionException` extend
  `IllegalStateException` instead of getting its own `@ExceptionHandler`
  method?"* — it lets one handler serve both the generic case and this
  specific one, since `IllegalStateException`'s built-in meaning ("object
  not in a state permitting this operation") is exactly the semantics of
  an illegal status transition — no need for a redundant handler.
- *"Why is the catch-all `Exception` handler important in production?"*
  — without it, an unhandled exception falls through to the servlet
  container's default error page (HTML, or a raw stack trace) instead of
  your API's JSON contract — a real information-disclosure and
  consistency risk.

---

## 10. Full HTTP status code reference (as requested)

| Code | Meaning | Used when |
|---|---|---|
| **200 OK** | Request succeeded, response has a body | `GET`, successful `PUT`, successful `PATCH` |
| **201 Created** | A new resource was created | Successful `POST /loans`, with `Location` header |
| **204 No Content** | Succeeded, deliberately no body | Successful `DELETE /loans/{id}` |
| **400 Bad Request** | Client sent malformed/invalid data | Bean Validation failures on request body |
| **401 Unauthorized** | Caller's identity isn't established | No/invalid auth token (not implemented in this demo — would sit in a Spring Security filter chain ahead of the controller) |
| **403 Forbidden** | Caller is known but not allowed | Authenticated user lacks the required role/permission (also a Security-layer concern, not shown here) |
| **404 Not Found** | The resource doesn't exist | Loan id not found on `GET/PUT/DELETE/PATCH` |
| **409 Conflict** | Request is valid but conflicts with current resource state | Illegal loan status transition |
| **500 Internal Server Error** | Unexpected server-side failure | Anything not explicitly handled — the safety net |

**Interview note on 401 vs 403:** 401 = "I don't know who you are" (or
your credentials are invalid) → *authenticate*. 403 = "I know who you
are, but you're not allowed to do this" → *authorize*. This project
doesn't implement Spring Security, but a real production version of this
API absolutely would (e.g. only `ROLE_LOAN_OFFICER` can call the status
PATCH endpoint) — worth saying explicitly if asked "where's your auth?"

---

## 11. Validation (`jakarta.validation` annotations)

| Annotation | Field | Why |
|---|---|---|
| `@NotBlank` | `applicantName`, `mobileNumber`, `email` | Rejects null AND empty/whitespace-only strings (unlike `@NotNull`, which allows `""`) |
| `@Email` | `email` | Format validation |
| `@Pattern` | `mobileNumber` | Enforces a realistic 10-digit mobile format |
| `@Positive` | `loanAmount`, `tenureMonths` | Business rule: these must be > 0 |
| `@NotNull` | `loanType`, `interestRate`, `tenureMonths`, `status` (on `StatusUpdateDTO`) | Required fields where blank-string checks don't apply (enums, numbers) |
| `@DecimalMin`/`@DecimalMax` | `interestRate` | Keeps the rate within a sane business range |
| `@Digits` | `loanAmount` | Bounds the number of integer/fraction digits to match the DB column's `precision`/`scale` |

**Common interviewer question:** *"Where does `@Valid` actually trigger
validation?"* — Spring MVC's `RequestResponseBodyMethodProcessor`
invokes the Bean Validation provider (Hibernate Validator) right after
deserializing the `@RequestBody`, before the controller method body
executes; a failure throws `MethodArgumentNotValidException` before your
code ever runs.

---

## 12. Pagination & sorting

`GET /api/v1/loans?page=0&size=10&sortBy=loanAmount&direction=asc`

- Built as a `PageRequest` in the controller from friendly query params,
  then passed straight through to `loanRepository.findAll(Pageable)`.
- Returns Spring's `Page<T>` (via `Page.map()` to convert entities to
  DTOs) which already serializes with `content`, `totalElements`,
  `totalPages`, `sort`, etc. — no manual pagination metadata to hand-roll.

**Common interviewer question:** *"Why not just accept a raw `Pageable`
parameter (Spring Data web support)?"* — you can (`Pageable pageable`
directly as a controller param with the `sort=field,dir` query
convention), and many production APIs do. This project exposes
`sortBy`/`direction` explicitly instead purely to demonstrate translating
a friendlier public contract into Spring Data's internal type — a
reasonable design choice either way, worth mentioning as a trade-off.

---

## 13. Logging (SLF4J via Lombok's `@Slf4j`)

Used at INFO for normal request flow (useful for tracing a request
through layers in production logs) and WARN/ERROR for rejected/failed
operations, so log aggregation tools (ELK/Splunk/CloudWatch) can alert on
WARN+ without being flooded by routine traffic.

**Best practice:** never log full request bodies containing PII (email,
mobile number) at INFO in a real system — this demo logs only ids/names
for illustration; production logging of applicant PII would typically be
masked or omitted entirely per compliance requirements.

---

## 14. What's intentionally out of scope (and why, if asked)

- **Spring Security / auth** — would sit in front of the controller
  layer as a filter chain; omitted here to keep focus on the CRUD +
  state-machine requirements, but 401/403 are documented above as if it
  existed.
- **Flyway/Liquibase migrations** — `schema-postgres-reference.sql` is
  provided as the canonical DDL; a real repo would wrap it in a
  versioned migration tool rather than relying on
  `hibernate.ddl-auto: update`.
- **Caching (`@Cacheable`)** — reasonable addition for `GET /loans/{id}`
  in a read-heavy system; omitted to keep the example focused.
