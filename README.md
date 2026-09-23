# Loan Approval System — Production-Style Spring Boot CRUD

Java 17 + Spring Boot 3.x + Spring Data JPA + MapStruct + Lombok + Bean
Validation. Built to interview standard: every class explains *why* it
exists, not just what it does. See `INTERVIEW_GUIDE.md` for the deep
class-by-class explanation, HTTP status code rationale, and common
interviewer questions.

## 1. Folder structure

```
loan-approval-system/
├── pom.xml
├── README.md
├── INTERVIEW_GUIDE.md
└── src/
    ├── main/
    │   ├── java/com/loanapp/
    │   │   ├── LoanApprovalSystemApplication.java
    │   │   ├── controller/
    │   │   │   └── LoanController.java
    │   │   ├── service/
    │   │   │   ├── LoanService.java
    │   │   │   └── impl/
    │   │   │       └── LoanServiceImpl.java
    │   │   ├── repository/
    │   │   │   └── LoanRepository.java
    │   │   ├── entity/
    │   │   │   ├── BaseEntity.java
    │   │   │   └── LoanApplication.java
    │   │   ├── dto/
    │   │   │   ├── ApiResponse.java
    │   │   │   ├── LoanRequestDTO.java
    │   │   │   ├── LoanResponseDTO.java
    │   │   │   └── StatusUpdateDTO.java
    │   │   ├── mapper/
    │   │   │   └── LoanMapper.java
    │   │   ├── enums/
    │   │   │   ├── LoanStatus.java
    │   │   │   └── LoanType.java
    │   │   ├── exception/
    │   │   │   ├── ResourceNotFoundException.java
    │   │   │   ├── InvalidStatusTransitionException.java
    │   │   │   ├── ErrorResponse.java
    │   │   │   └── GlobalExceptionHandler.java
    │   │   └── util/
    │   │       └── AppConstants.java
    │   └── resources/
    │       ├── application.yml
    │       └── schema-postgres-reference.sql
    └── test/
        └── java/com/loanapp/   (placeholder for unit/integration tests)
```

This is a strict **layered architecture**: controller → service → repository,
with entity/dto/mapper/enums/exception/util as cross-cutting supporting
packages. Each layer only talks to the layer directly below it — the
controller never touches the repository, and the repository never knows
DTOs exist.

## 2. Running it

Default profile is `dev`, which uses in-memory H2 — no external DB needed
to try it out.

```bash
mvn spring-boot:run
```

- App: `http://localhost:8080/api/v1/loans`
- H2 console: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:loandb`)

For Postgres, run with the `prod` profile and set env vars:

```bash
export DB_HOST=localhost DB_PORT=5432 DB_NAME=loan_db DB_USERNAME=loan_user DB_PASSWORD=secret
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

Apply `schema-postgres-reference.sql` via Flyway/Liquibase (or manually
for a quick test) — `ddl-auto: validate` in the prod profile deliberately
does **not** create tables for you.

## 3. Sample requests (Postman / curl)

### Create a loan — `POST /api/v1/loans`

```json
{
  "applicantName": "Ravi Kumar",
  "mobileNumber": "9876543210",
  "email": "ravi.kumar@example.com",
  "loanAmount": 500000,
  "loanType": "HOME",
  "interestRate": 8.5,
  "tenureMonths": 240
}
```

Response — `201 Created`, header `Location: /api/v1/loans/1`

Every successful response is wrapped in `ApiResponse<T>` — see
`dto/ApiResponse.java` for why the success envelope mirrors the shape of
`ErrorResponse` (`status` + `timestamp` on both, `data` on success vs.
`error`/`message`/`validationErrors` on failure):

```json
{
  "timestamp": "2026-09-11T10:15:30",
  "status": 201,
  "message": "Loan application created successfully",
  "data": {
    "id": 1,
    "applicantName": "Ravi Kumar",
    "mobileNumber": "9876543210",
    "email": "ravi.kumar@example.com",
    "loanAmount": 500000.00,
    "loanType": "HOME",
    "loanTypeDisplayName": "Home Loan",
    "status": "PENDING",
    "statusDisplayName": "Pending Review",
    "interestRate": 8.50,
    "tenureMonths": 240,
    "createdAt": "2026-09-11T10:15:30",
    "updatedAt": "2026-09-11T10:15:30"
  }
}
```

### Validation failure — missing/invalid fields

`POST /api/v1/loans` with `"email": "not-an-email"` and no `applicantName`
→ `400 Bad Request`:

```json
{
  "timestamp": "2026-09-11T10:16:02",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for one or more fields",
  "path": "/api/v1/loans",
  "validationErrors": {
    "applicantName": "Applicant name is required",
    "email": "Email must be a valid email address"
  }
}
```

### Get one — `GET /api/v1/loans/1` → `200 OK` (or `404` shape below)

```json
{
  "timestamp": "2026-09-11T10:17:00",
  "status": 404,
  "error": "Not Found",
  "message": "Loan not found with id 5",
  "path": "/api/v1/loans/5"
}
```

### List with pagination + sorting

`GET /api/v1/loans?page=0&size=10&sortBy=loanAmount&direction=desc`

The `Page<LoanResponseDTO>` becomes the `data` payload — its own
pagination metadata (`totalElements`, `totalPages`, `sort`, ...) is
untouched, just nested one level deeper under the envelope:

```json
{
  "timestamp": "2026-09-11T10:18:40",
  "status": 200,
  "message": "Loans fetched successfully",
  "data": {
    "content": [ { "id": 3, "loanAmount": 900000.00, "...": "..." } ],
    "pageable": { "pageNumber": 0, "pageSize": 10 },
    "totalElements": 3,
    "totalPages": 1,
    "last": true,
    "first": true,
    "sort": { "sorted": true, "unsorted": false }
  }
}
```

### Update editable fields — `PUT /api/v1/loans/1` → `200 OK`

Same body shape as create; `status` is not accepted here. Response is
wrapped the same way, with `"message": "Loan application updated
successfully"`.

### Change status — `PATCH /api/v1/loans/1/status`

```json
{ "status": "UNDER_REVIEW" }
```

`200 OK` on a legal transition (`PENDING -> UNDER_REVIEW`):

```json
{
  "timestamp": "2026-09-11T10:20:11",
  "status": 200,
  "message": "Loan status updated to Under Review",
  "data": { "id": 1, "status": "UNDER_REVIEW", "...": "..." }
}
```

Trying `PENDING -> APPROVED` directly (skipping review) →
`409 Conflict`:

```json
{
  "timestamp": "2026-09-11T10:20:11",
  "status": 409,
  "error": "Conflict",
  "message": "Cannot move loan from status 'PENDING' to 'APPROVED'. Allowed next statuses: [UNDER_REVIEW, REJECTED]",
  "path": "/api/v1/loans/1/status"
}
```

### Delete — `DELETE /api/v1/loans/1` → `204 No Content`

## 4. Maven dependencies (see `pom.xml`)

| Dependency | Purpose |
|---|---|
| `spring-boot-starter-web` | REST controllers, embedded Tomcat |
| `spring-boot-starter-validation` | Bean Validation (`@NotBlank`, `@Email`, ...) |
| `spring-boot-starter-data-jpa` | Spring Data repositories, Hibernate |
| `postgresql` | Production DB driver |
| `h2` | In-memory DB for the `dev` profile |
| `lombok` | Removes getter/setter/constructor boilerplate |
| `mapstruct` + `mapstruct-processor` | Compile-time entity↔DTO mapping |
| `lombok-mapstruct-binding` | Makes Lombok-generated accessors visible to MapStruct's annotation processor (must be on the annotation processor path, and Lombok must run before MapStruct — see `pom.xml` comments) |
| `spring-boot-starter-actuator` | `/actuator/health` for readiness/liveness probes |
| `spring-boot-starter-test` | JUnit 5, Mockito, MockMvc |

Full class-by-class rationale, HTTP status code usage, and likely
interview questions are in **`INTERVIEW_GUIDE.md`**.
