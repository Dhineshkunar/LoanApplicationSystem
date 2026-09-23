package com.loanapp.service.impl;

import com.loanapp.dto.LoanRequestDTO;
import com.loanapp.dto.LoanResponseDTO;
import com.loanapp.entity.LoanApplication;
import com.loanapp.enums.LoanStatus;
import com.loanapp.exception.InvalidStatusTransitionException;
import com.loanapp.exception.ResourceNotFoundException;
import com.loanapp.mapper.LoanMapper;
import com.loanapp.repository.LoanRepository;
import com.loanapp.service.LoanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for loan applications.
 *
 * WHY LOGIC LIVES HERE AND NOT IN THE CONTROLLER OR REPOSITORY:
 * - Controllers should only translate HTTP <-> DTOs and delegate; putting
 *   business rules there makes them untestable without spinning up the
 *   whole web layer.
 * - Repositories should only know about persistence, not business rules
 *   like "PENDING can only move to UNDER_REVIEW or REJECTED" — that rule
 *   belongs to the domain (modeled on the enum itself) and is ENFORCED
 *   here, in the layer responsible for orchestrating a use case.
 *
 * @Transactional AT THE CLASS LEVEL:
 * Every public method runs inside a database transaction by default.
 * - readOnly = true is set at the class level as the common case (loan
 *   reads), which lets Hibernate skip dirty-checking overhead and lets
 *   the DB driver apply read-only optimizations.
 * - Write methods (create/update/delete/status-change) override this
 *   with their own @Transactional (readOnly = false is the default), so
 *   they get a real read-write transaction that rolls back automatically
 *   if a RuntimeException propagates out of the method — e.g. if
 *   InvalidStatusTransitionException is thrown mid-method, nothing is
 *   committed.
 *
 * INTERVIEW Q: "Where should @Transactional go — controller or service?"
 * Always the service layer. Controllers deal in HTTP semantics; a single
 * business use case (which may touch multiple repositories) is the
 * natural transactional boundary, and that boundary is the service
 * method.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoanServiceImpl implements LoanService {

    private final LoanRepository loanRepository;
    private final LoanMapper loanMapper;

    @Override
    @Transactional
    public LoanResponseDTO createLoan(LoanRequestDTO requestDTO) {
        log.info("Creating loan application for applicant: {}", requestDTO.getApplicantName());

        LoanApplication entity = loanMapper.toEntity(requestDTO);
        // Business rule: every new application starts life as PENDING.
        // This is deliberately set here, NOT accepted from the client —
        // see LoanRequestDTO's javadoc for why status isn't a request field.
        entity.setStatus(LoanStatus.PENDING);

        LoanApplication saved = loanRepository.save(entity);
        log.info("Loan application created with id: {}", saved.getId());

        return loanMapper.toResponseDTO(saved);
    }

    @Override
    public LoanResponseDTO getLoanById(Long id) {
        LoanApplication entity = findEntityOrThrow(id);
        return loanMapper.toResponseDTO(entity);
    }

    @Override
    public Page<LoanResponseDTO> getAllLoans(Pageable pageable) {
        Page<LoanApplication> page = loanRepository.findAll(pageable);
        // Page.map() preserves all pagination metadata (totalElements,
        // totalPages, sort, etc.) while transforming the content list —
        // avoids manually rebuilding a PageImpl by hand.
        return page.map(loanMapper::toResponseDTO);
    }

    @Override
    @Transactional
    public LoanResponseDTO updateLoan(Long id, LoanRequestDTO requestDTO) {
        log.info("Updating loan application id: {}", id);

        LoanApplication entity = findEntityOrThrow(id);
        // Mutates the managed entity in place; Hibernate's dirty checking
        // flushes the changes automatically at transaction commit — no
        // explicit repository.save() call is required here (though calling
        // it is harmless and some teams do so for readability).
        loanMapper.updateEntityFromDto(requestDTO, entity);
        loanRepository.save(entity); // Explicit save for readability (optional)
        return loanMapper.toResponseDTO(entity);
    }

    @Override
    @Transactional
    public LoanResponseDTO updateLoanStatus(Long id, LoanStatus targetStatus) {
        LoanApplication entity = findEntityOrThrow(id);
        LoanStatus current = entity.getStatus();

        log.info("Attempting status transition for loan {}: {} -> {}", id, current, targetStatus);

        if (!current.canMoveTo(targetStatus)) {
            log.warn("Rejected invalid transition for loan {}: {} -> {}", id, current, targetStatus);
            throw new InvalidStatusTransitionException(current, targetStatus);
        }

        entity.setStatus(targetStatus);
        return loanMapper.toResponseDTO(entity);
    }

    @Override
    @Transactional
    public void deleteLoan(Long id) {
        if (!loanRepository.existsById(id)) {
            throw ResourceNotFoundException.forLoan(id);
        }
        loanRepository.deleteById(id);
        log.info("Deleted loan application id: {}", id);
    }

    private LoanApplication findEntityOrThrow(Long id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forLoan(id));
    }
}
