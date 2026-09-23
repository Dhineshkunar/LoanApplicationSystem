package com.loanapp.dto;

import com.loanapp.enums.LoanStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload for PATCH /loans/{id}/status.
 *
 * A dedicated, minimal DTO for this endpoint (rather than reusing
 * LoanRequestDTO with other fields ignored) makes the API contract
 * explicit: this endpoint ONLY changes status, nothing else. This also
 * means Bean Validation only checks what's relevant to this operation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StatusUpdateDTO {

    @NotNull(message = "Target status is required")
    private LoanStatus status;
}
