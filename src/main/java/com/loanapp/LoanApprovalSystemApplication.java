package com.loanapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Entry point of the Loan Approval System.
 *
 * @EnableJpaAuditing turns on automatic population of @CreatedDatpe /
 * @LastModifiedDate fields (see BaseEntity/LoanApplication) without the
 * service layer ever touching timestamps manually. This is the standard
 * production pattern — timestamps set by hand in service code are a common
 * source of bugs (forgotten update, wrong timezone, etc).
 */
@SpringBootApplication
@EnableJpaAuditing
public class LoanApprovalSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoanApprovalSystemApplication.class, args);
    }
}
