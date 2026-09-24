package com.loanapp.controller;

import com.loanapp.dto.*;
import com.loanapp.entity.LoanApplication;
import com.loanapp.service.LoanService;
import com.loanapp.specification.LoanApplicationSpecification;
import com.loanapp.util.AppConstants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * REST controller for Loan Application CRUD + status transitions.
 *
 * WHY THE CONTROLLER STAYS THIN:
 * Every method here does exactly three things: accept/validate input,
 * delegate to LoanService, wrap the result in the right ResponseEntity.
 * No business rules, no direct repository access. This keeps the HTTP
 * layer trivially testable with MockMvc/WebMvcTest and keeps business
 * logic reusable outside the web layer (e.g. from a batch job or CLI).
 *
 * API VERSIONING:
 * The base path is /api/v1/loans, not just /loans. URI versioning is the
 * simplest and most widely used strategy in interviews and practice —
 * it's visible, cache-friendly, and trivial to route at a gateway/proxy
 * level. (Alternatives: a custom header like "X-API-Version", or content
 * negotiation via the Accept header — both valid, but add complexity most
 * teams don't need.)
 *
 * RESPONSE ENVELOPE:
 * Every 2xx response body is wrapped in {@link ApiResponse}, the success
 * counterpart to {@link com.loanapp.exception.ErrorResponse} (which
 * GlobalExceptionHandler already uses for every failure path). See
 * ApiResponse's javadoc for why both success and error responses share
 * a "status" + "timestamp" envelope shape. The one deliberate exception
 * is DELETE, which returns 204 No Content — a wrapper object would
 * contradict "no content" by definition, so it stays a bare empty body.
 */
@Slf4j
@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/loans")
@RequiredArgsConstructor
@CrossOrigin
public class LoanController {

    private final LoanService loanService;

    /**
     * POST /api/v1/loans
     *
     * Returns 201 Created with a Location header pointing at the new
     * resource's URI — the correct REST convention for a successful
     * creation, not just 200 OK with a body.
     */
    @PostMapping("/createLoanParticipant")
    public ResponseEntity<ApiResponse<LoanResponseDTO>> createLoan(
            @Valid @RequestBody LoanRequestDTO requestDTO) {

        log.info("POST /loans - creating new loan application");
        LoanResponseDTO created = loanService.createLoan(requestDTO);

        URI location = URI.create(AppConstants.API_BASE_PATH + "/loans/" + created.getId());
        ApiResponse<LoanResponseDTO> body = ApiResponse.of(
                HttpStatus.CREATED.value(), "Loan application created successfully", created);

        return ResponseEntity.created(location).body(body);
    }

    /**
     * GET /api/v1/loans/{id}
     * 200 OK, or the GlobalExceptionHandler turns a
     * ResourceNotFoundException from the service into 404 automatically —
     * notice there's no try/catch here at all.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LoanResponseDTO>> getLoanById(@PathVariable Long id) {
        log.info("GET /loans/{}", id);
        LoanResponseDTO loan = loanService.getLoanById(id);

        return ResponseEntity.ok(
                ApiResponse.of(HttpStatus.OK.value(), "Loan fetched successfully", loan));
    }

    /**
     * GET /api/v1/loans?page=0&size=10&sortBy=loanAmount&direction=asc
     *
     * Pageable is built manually here (rather than letting Spring Data
     * bind a Pageable argument directly) so the endpoint can expose
     * friendlier query param names (sortBy/direction) instead of Spring
     * Data's default "sort=field,dir" syntax, which is a very common
     * production/interview customization.
     *
     * The paged content itself (Page<LoanResponseDTO>) becomes the "data"
     * payload inside ApiResponse — Page already carries its own
     * pagination metadata (totalElements, totalPages, sort...), so
     * wrapping it just adds the top-level status/message/timestamp
     * envelope on top without duplicating or losing that metadata.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<LoanResponseDTO>>> getAllLoans(
            @Valid @ModelAttribute LoanSearchRequest loanSearchRequest){


       List<LoanResponseDTO> loans = loanService.getAllLoans(loanSearchRequest);

        return ResponseEntity.ok(
                ApiResponse.of(HttpStatus.OK.value(), "Loans fetched successfully", loans));
    }

    /**
     * PUT /api/v1/loans/{id}
     * Full update of the editable business fields (not status — that's a
     * separate, narrower operation via PATCH, see below).
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<LoanResponseDTO>> updateLoan(
            @PathVariable Long id, @Valid @RequestBody LoanRequestDTO requestDTO) {

        log.info("PUT /loans/{}", id);
        LoanResponseDTO updated = loanService.updateLoan(id, requestDTO);

        return ResponseEntity.ok(
                ApiResponse.of(HttpStatus.OK.value(), "Loan application updated successfully", updated));
    }

    /**
     * PATCH /api/v1/loans/{id}/status
     *
     * WHY PATCH AND A SEPARATE SUB-RESOURCE PATH, NOT PART OF PUT:
     * Status changes are a distinct business operation with its own
     * validation rules (the state machine), not just "update one field of
     * the record." Modeling it as its own endpoint makes the transition
     * rule impossible to bypass by smuggling a status value into a
     * general-purpose PUT, and lets this endpoint return 409 Conflict
     * specifically for illegal transitions without conflating that with
     * general validation failures on PUT.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<LoanResponseDTO>> updateLoanStatus(
            @PathVariable Long id, @Valid @RequestBody StatusUpdateDTO statusUpdateDTO) {

        log.info("PATCH /loans/{}/status -> {}", id, statusUpdateDTO.getStatus());
        LoanResponseDTO updated = loanService.updateLoanStatus(id, statusUpdateDTO.getStatus());

        String message = "Loan status updated to " + updated.getStatusDisplayName();
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK.value(), message, updated));
    }

    /**
     * DELETE /api/v1/loans/{id}
     * 204 No Content: the operation succeeded and there is intentionally
     * no response body to return. Deliberately NOT wrapped in ApiResponse
     * — a 204 is defined by RFC 9110 as having no message body at all, so
     * wrapping it would mean either violating that (sending a body on a
     * "no content" status) or sending an empty-but-present ApiResponse
     * shell, which just confuses clients that correctly expect nothing
     * here. This is the one endpoint where "no body" IS the clean,
     * correct contract.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLoan(@PathVariable Long id) {
        log.info("DELETE /loans/{}", id);
        loanService.deleteLoan(id);
        return ResponseEntity.noContent().build();
    }
}
