package com.loanapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Common audit columns shared by every entity in the system.
 *
 * WHY A BASE CLASS:
 * Every production table needs createdAt/updatedAt. Duplicating those two
 * fields (plus the auditing annotations) into every entity is repetitive
 * and easy to get subtly wrong (one entity uses OffsetDateTime, another
 * LocalDateTime...). A @MappedSuperclass centralizes it once; new entities
 * just extend this class and get auditing for free.
 *
 * INTERVIEW Q: "@MappedSuperclass vs @Entity + @Inheritance?"
 * @MappedSuperclass does NOT create its own table and has no identity of
 * its own — it only contributes columns to subclass tables. Use it for
 * cross-cutting fields like this. Use @Inheritance (SINGLE_TABLE / JOINED
 * / TABLE_PER_CLASS) when subclasses are genuinely polymorphic domain
 * entities you need to query/persist as a family.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
