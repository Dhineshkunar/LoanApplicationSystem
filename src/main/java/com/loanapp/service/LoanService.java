package com.loanapp.service;

import com.loanapp.dto.LoanRequestDTO;
import com.loanapp.dto.LoanResponseDTO;
import com.loanapp.dto.LoanSearchRequest;
import com.loanapp.entity.LoanApplication;
import com.loanapp.enums.LoanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * Service-layer contract for loan operations.
 *
 * WHY AN INTERFACE + IMPL, RATHER THAN PUTTING @Service DIRECTLY ON ONE
 * CLASS:
 * - Lets the controller depend on an abstraction, not a concrete class —
 *   standard Dependency Inversion. Makes unit testing the controller
 *   trivial (mock the interface, no Spring context needed).
 * - Keeps the door open for multiple implementations (e.g. a caching
 *   decorator, or a different implementation per profile) without
 *   touching callers.
 * - Some teams skip this for simple CRUD services since Spring beans are
 *   typically proxied by interface anyway when needed (e.g. for AOP); it's
 *   shown here because it's still a very common production convention and
 *   a common interview discussion point ("do you always need an interface
 *   for a Spring service?" — answer: not always, but it's cheap insurance
 *   and this codebase demonstrates the pattern explicitly).
 */
public interface LoanService {

    LoanResponseDTO createLoan(LoanRequestDTO requestDTO);

    LoanResponseDTO getLoanById(Long id);

    List<LoanResponseDTO> getAllLoans(LoanSearchRequest loanSearchRequest);

    LoanResponseDTO updateLoan(Long id, LoanRequestDTO requestDTO);

    LoanResponseDTO updateLoanStatus(Long id, LoanStatus targetStatus);

    void deleteLoan(Long id);
}
