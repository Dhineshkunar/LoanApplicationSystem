package com.loanapp.specification;

import com.loanapp.entity.LoanApplication;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class LoanApplicationSpecification {
    public static Specification<LoanApplication> getSpecification( String search) {
        return new Specification<LoanApplication>() {
            @Override
            public Predicate toPredicate(Root<LoanApplication> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                if(search==null || search.isEmpty()){
                    return criteriaBuilder.conjunction();
                }
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(criteriaBuilder.like(root.get("applicantName"),"%"+search+"%"));
                return  criteriaBuilder.or(predicates.toArray(new Predicate[0]));
            }
        };
    }
}
