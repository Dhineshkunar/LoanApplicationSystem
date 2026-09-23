package com.loanapp.mapper;

import com.loanapp.dto.LoanRequestDTO;
import com.loanapp.dto.LoanResponseDTO;
import com.loanapp.entity.LoanApplication;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.NullValueCheckStrategy;

/**
 * MapStruct mapper: entity <-> DTO conversions.
 *
 * WHY MAPSTRUCT INSTEAD OF HAND-WRITTEN MAPPING METHODS (interview point):
 *
 * 1. COMPILE-TIME GENERATION, NOT REFLECTION.
 *    Unlike ModelMapper/Dozer/BeanUtils, which map fields via reflection
 *    at RUNTIME, MapStruct is an annotation processor: it generates a
 *    plain Java class (LoanMapperImpl) at COMPILE time that does exactly
 *    what a hand-written mapper would — direct getter/setter calls.
 *    Result: no reflection overhead, and mapping bugs (typo'd field name,
 *    incompatible type) are COMPILE ERRORS, not runtime surprises
 *    discovered in production.
 *
 * 2. LESS BOILERPLATE THAN HAND-WRITTEN MAPPERS.
 *    A hand-written mapper for LoanApplication would still need ~15 lines
 *    of getter/setter calls per direction, repeated for every entity in
 *    the codebase. MapStruct generates that mechanically and lets you
 *    override ONLY the fields that need custom logic (see the enum
 *    display-name mapping below) — you write the 10% that's actually
 *    interesting.
 *
 * 3. REFACTOR-SAFE.
 *    Rename a field on the entity and forget to update a hand-written
 *    mapper -> silent bug (old value stays null/stale) discovered by a QA
 *    ticket. Same rename with MapStruct -> the generated code no longer
 *    compiles, caught immediately in CI.
 *
 * 4. TESTABLE / DEBUGGABLE LIKE ANY OTHER JAVA CLASS.
 *    You can open target/generated-sources and read LoanMapperImpl.java
 *    like normal code, set a breakpoint in it, etc. — it's not a black
 *    box the way runtime-reflection mappers are.
 *
 * componentModel = "spring" makes MapStruct generate the impl as a Spring
 * @Component, so it can simply be @Autowired / constructor-injected into
 * the service layer like any other bean.
 */
@Mapper(componentModel = "spring")
public interface LoanMapper {

    /**
     * Request DTO -> new entity for creation.
     * status/id/createdAt/updatedAt are intentionally NOT part of
     * LoanRequestDTO, so MapStruct maps only the fields that exist on
     * both sides — id and audit fields on the entity are simply left at
     * their Java defaults (null), to be set by the service layer /
     * auditing infrastructure.
     */
    LoanApplication toEntity(LoanRequestDTO dto);

    /**
     * Entity -> response DTO.
     * @Mapping calls handle the two fields that don't have a same-named
     * counterpart on the entity: the human-readable display names, which
     * MapStruct can't infer automatically since they come from enum
     * methods, not enum fields.
     */
    @Mapping(target = "loanType", expression = "java(entity.getLoanType().name())")
    @Mapping(target = "loanTypeDisplayName", expression = "java(entity.getLoanType().getDisplayName())")
    @Mapping(target = "status", expression = "java(entity.getStatus().name())")
    @Mapping(target = "statusDisplayName", expression = "java(entity.getStatus().getDisplayName())")
    LoanResponseDTO toResponseDTO(LoanApplication entity);

    /**
     * Applies editable fields from an update request onto an EXISTING,
     * already-persisted entity (used by PUT /loans/{id}).
     *
     * @MappingTarget tells MapStruct to mutate the given entity in place
     * rather than construct a new one — essential here because the
     * managed entity must keep its id/status/audit fields untouched;
     * only the fields present on LoanRequestDTO get overwritten.
     *
     * NullValuePropertyMappingStrategy.IGNORE means if a source field is
     * null, the target's existing value is left alone rather than
     * overwritten with null — relevant if this DTO is ever reused for a
     * PATCH-style partial update in the future.
     */
    @BeanMapping(
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
            nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS
    )
    void updateEntityFromDto(LoanRequestDTO dto, @MappingTarget LoanApplication entity);
}
