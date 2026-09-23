package com.loanapp.dto;

import com.loanapp.enums.LoanType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Inbound payload for POST /loans and PUT /loans/{id}.
 *
 * WHY A SEPARATE DTO INSTEAD OF EXPOSING THE ENTITY DIRECTLY:
 * - The entity has fields (id, status, createdAt, updatedAt) that a
 *   client creating a loan should never set directly — status starts
 *   at PENDING by business rule, id is generated, timestamps are audited.
 *   Exposing the entity means a malicious or buggy client could set
 *   status=APPROVED on creation.
 * - Validation annotations belong on the INPUT boundary, not the entity.
 *   The entity may be loaded from the DB in a state that wouldn't pass
 *   "creation" validation (e.g. a closed loan) — conflating the two
 *   causes exactly this kind of bug.
 * - It decouples the API contract from the DB schema: the table can be
 *   refactored without breaking clients, and vice versa.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanRequestDTO {

    @NotBlank(message = "Applicant name is required")
    @Size(max = 100, message = "Applicant name must not exceed 100 characters")
    private String applicantName;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Mobile number must be a valid 10-digit number")
    private String mobileNumber;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    @NotNull(message = "Loan amount is required")
    @Positive(message = "Loan amount must be greater than zero")
    @Digits(integer = 13, fraction = 2, message = "Loan amount format is invalid")
    private BigDecimal loanAmount;

    @NotNull(message = "Loan type is required")
    private LoanType loanType;

    @NotNull(message = "Interest rate is required")
    @DecimalMin(value = "0.01", message = "Interest rate must be greater than zero")
    @DecimalMax(value = "50.0", message = "Interest rate looks unrealistic")
    private BigDecimal interestRate;

    @NotNull(message = "Tenure in months is required")
    @Positive(message = "Tenure must be greater than zero")
    @Max(value = 480, message = "Tenure cannot exceed 480 months")
    private Integer tenureMonths;
}
