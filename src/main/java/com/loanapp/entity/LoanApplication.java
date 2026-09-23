package com.loanapp.entity;

import com.loanapp.enums.LoanStatus;
import com.loanapp.enums.LoanType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * JPA entity mapping to the loan_applications table.
 *
 * DESIGN DECISIONS TO CALL OUT IN AN INTERVIEW:
 *
 * 1. Enums stored as STRING, not ORDINAL.
 *    ORDINAL stores the enum's position (0,1,2...) as an int. If someone
 *    later reorders or inserts a constant in the enum, every existing row
 *    silently means something different. STRING stores the constant name
 *    ("APPROVED"), which is safe to reorder and human-readable in the DB.
 *
 * 2. BigDecimal for money and interest rate, never double/float.
 *    Binary floating point cannot represent most decimal fractions
 *    exactly, which is unacceptable for currency and interest
 *    calculations. BigDecimal with an explicit scale is the standard.
 *
 * 3. No manual createdAt/updatedAt here — inherited from BaseEntity and
 *    populated by Spring Data JPA auditing (@EnableJpaAuditing on the main
 *    class), so the service layer never has to remember to set them.
 *
 * 4. Lombok's @Data is intentionally AVOIDED on entities. @Data generates
 *    equals()/hashCode() over all fields and toString() including
 *    associations, which can trigger unwanted lazy-loading, break
 *    JPA-managed identity semantics, and cause StackOverflow with
 *    bidirectional relations. We use targeted @Getter/@Setter instead.
 */
@Entity
@Table(name = "loan_applications", indexes = {
        @Index(name = "idx_loan_status", columnList = "status"),
        @Index(name = "idx_loan_type", columnList = "loan_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(callSuper = true)
public class LoanApplication extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "applicant_name", nullable = false, length = 100)
    private String applicantName;

    @Column(name = "mobile_number", nullable = false, length = 15)
    private String mobileNumber;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Column(name = "loan_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal loanAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "loan_type", nullable = false, length = 20)
    private LoanType loanType;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false, length = 20)
    private LoanStatus status;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "tenure_months", nullable = false)
    private Integer tenureMonths;
}
