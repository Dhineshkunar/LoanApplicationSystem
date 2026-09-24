package com.loanapp.repository;

import com.loanapp.entity.LoanApplication;
import com.loanapp.enums.LoanStatus;
import com.loanapp.enums.LoanType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Data access layer for LoanApplication.
 *
 * WHY EXTEND JpaRepository INSTEAD OF WRITING DAO CLASSES BY HAND:
 * - JpaRepository<LoanApplication, Long> already provides save, findById,
 *   findAll(Pageable), deleteById, existsById, count, etc. — all backed
 *   by Spring Data's dynamic proxy implementation, with ZERO
 *   implementation code required here.
 * - Derived query methods (findByStatus, findByLoanType below) are parsed
 *   from the METHOD NAME at startup and turned into JPQL automatically —
 *   no hand-written SQL/JPQL, no risk of typos in a query string.
 * - Pagination/sorting come for free via the Pageable parameter, which
 *   Spring Data translates into LIMIT/OFFSET + ORDER BY at the SQL level
 *   (not fetching everything into memory and paging in Java).
 *
 * INTERVIEW Q: "How does Spring know what SQL to generate for
 * findByStatus(LoanStatus status)?"
 * At application startup, Spring Data parses the method name against the
 * entity's metamodel ("status" matches the LoanApplication.status field),
 * builds a JPQL query, and generates a proxy implementation — this
 * happens once at boot, not per-call.
 */
@Repository
public interface LoanRepository extends JpaRepository<LoanApplication, Long>, JpaSpecificationExecutor<LoanApplication> {

    Page<LoanApplication> findByStatus(LoanStatus status, Pageable pageable);

    Page<LoanApplication> findByLoanType(LoanType loanType, Pageable pageable);

    Page<LoanApplication> findByStatusAndLoanType(
            LoanStatus status, LoanType loanType, Pageable pageable);
}
