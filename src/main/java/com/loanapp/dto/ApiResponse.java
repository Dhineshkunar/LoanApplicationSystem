package com.loanapp.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Uniform SUCCESS envelope for every 2xx response in the API — the
 * deliberate counterpart to {@link com.loanapp.exception.ErrorResponse},
 * which already covers every failure path.
 *
 * WHY WRAP SUCCESS RESPONSES TOO, NOT JUST ERRORS:
 * Before this class, a 404 returned:
 *   { "status": 404, "error": "Not Found", "message": "...", "path": "..." }
 * ...but a 200 returned the bare DTO:
 *   { "id": 1, "applicantName": "Ravi Kumar", ... }
 *
 * Two different top-level shapes for the same endpoint (depending on
 * success/failure) forces frontend code to branch on HTTP status BEFORE
 * it can even start reading the body consistently, and makes it
 * impossible to add cross-cutting metadata (a message, a server
 * timestamp, a trace id) to successful responses later without breaking
 * every existing client. Wrapping BOTH paths in a small, predictable
 * envelope is the standard production fix:
 *
 *   200 -> { "status": 200, "message": "Loan fetched successfully",
 *            "data": { ...actual DTO... }, "timestamp": "..." }
 *   404 -> { "status": 404, "error": "Not Found", "message": "...",
 *            "path": "...", "timestamp": "..." }
 *
 * The client's parsing rule becomes trivially uniform: "status" and
 * "timestamp" are always present; on success read "data", on failure
 * read "error"/"message"/"validationErrors" (from ErrorResponse).
 *
 * WHY THIS STAYS A SEPARATE CLASS FROM ErrorResponse (NOT ONE SHARED
 * "ApiResponse" WITH NULLABLE success/error FIELDS):
 * Success and failure carry genuinely different data (a payload vs. an
 * error code + validation map). Forcing them into one class means most
 * fields are null depending on the branch, which is exactly the
 * "nullable soup" DTO shape production teams try to avoid. Two small,
 * fully-populated classes are easier to reason about than one
 * half-empty one — @JsonInclude(NON_NULL) on both keeps the wire format
 * clean either way.
 *
 * GENERICS (ApiResponse<T>):
 * T is whatever the endpoint actually returns — LoanResponseDTO for a
 * single loan, Page<LoanResponseDTO> for the list endpoint — so this one
 * class serves every controller method without duplicating a wrapper per
 * DTO type.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    private int status;
    private String message;
    private T data;

    /** Convenience factory for the common "200 OK with a message" case. */
    public static <T> ApiResponse<T> of(int status, String message, T data) {
        return ApiResponse.<T>builder()
                .status(status)
                .message(message)
                .data(data)
                .build();
    }
}
