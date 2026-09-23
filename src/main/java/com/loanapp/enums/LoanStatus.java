package com.loanapp.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * Represents the lifecycle of a loan application.
 *
 * WHY AN ENUM WITH BEHAVIOR (not just a String column):
 * A plain String status field lets any layer set any value at any time
 * ("APPROVED" -> "PENDING" is nonsensical business-wise but a String can't
 * stop it). Modeling the state machine INSIDE the enum means the allowed
 * transitions live in exactly one place, are compile-time checked, and are
 * impossible to bypass accidentally from a typo in a controller or SQL
 * migration.
 *
 * INTERVIEW TALKING POINT:
 * This is a textbook "State" pattern implemented with a Java enum instead
 * of a full class hierarchy — appropriate here because the states are a
 * closed, fixed set known at compile time.
 */
public enum LoanStatus {

    PENDING("PENDING", "Pending Review") {
        @Override
        public Set<LoanStatus> nextAllowed() {
            return EnumSet.of(UNDER_REVIEW, REJECTED);
        }
    },
    UNDER_REVIEW("UNDER_REVIEW", "Under Review") {
        @Override
        public Set<LoanStatus> nextAllowed() {
            return EnumSet.of(APPROVED, REJECTED);
        }
    },
    APPROVED("APPROVED", "Approved") {
        @Override
        public Set<LoanStatus> nextAllowed() {
            return EnumSet.of(DISBURSED, REJECTED);
        }
    },
    REJECTED("REJECTED", "Rejected") {
        @Override
        public Set<LoanStatus> nextAllowed() {
            // Terminal state — no further transitions permitted.
            return EnumSet.noneOf(LoanStatus.class);
        }
    },
    DISBURSED("DISBURSED", "Disbursed") {
        @Override
        public Set<LoanStatus> nextAllowed() {
            return EnumSet.of(CLOSED);
        }
    },
    CLOSED("CLOSED", "Closed") {
        @Override
        public Set<LoanStatus> nextAllowed() {
            // Terminal state.
            return EnumSet.noneOf(LoanStatus.class);
        }
    };

    private final String dbValue;
    private final String displayName;

    LoanStatus(String dbValue, String displayName) {
        this.dbValue = dbValue;
        this.displayName = displayName;
    }

    public String getDbValue() {
        return dbValue;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * The set of statuses this status is permitted to move to next.
     * Each enum constant overrides this with its own transition table.
     */
    public abstract Set<LoanStatus> nextAllowed();

    /**
     * Helper method requested explicitly in the spec: validates whether a
     * transition from "this" status to {@code target} is legal.
     *
     * Used by the service layer BEFORE persisting a status change, so an
     * illegal transition never reaches the database.
     */
    public boolean canMoveTo(LoanStatus target) {
        return this.nextAllowed().contains(target);
    }
}
