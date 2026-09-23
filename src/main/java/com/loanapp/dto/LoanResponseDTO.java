package com.loanapp.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Outbound representation returned to clients.
 *
 * Note it exposes DISPLAY-FRIENDLY strings for status/loanType
 * (statusDisplayName, loanTypeDisplayName) alongside the raw enum name.
 * Frontends typically want both: the raw code for logic/comparisons, and
 * the human label for showing in UI — this avoids the frontend
 * hardcoding a status->label lookup table that can drift from the backend.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanResponseDTO {

    private Long id;
    private String applicantName;
    private String mobileNumber;
    private String email;
    private BigDecimal loanAmount;

    private String loanType;
    private String loanTypeDisplayName;

    private String status;
    private String statusDisplayName;

    private BigDecimal interestRate;
    private Integer tenureMonths;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
