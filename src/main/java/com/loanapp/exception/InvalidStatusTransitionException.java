package com.loanapp.exception;

import com.loanapp.enums.LoanStatus;

/**
 * Thrown when a requested loan status transition violates the state
 * machine defined in {@link LoanStatus#canMoveTo(LoanStatus)}.
 *
 * Deliberately extends IllegalStateException (rather than a brand-new
 * root type) because "the object is being asked to do something it
 * cannot do in its current state" is precisely what IllegalStateException
 * means in the JDK — GlobalExceptionHandler maps IllegalStateException to
 * 409 Conflict, which is exactly the semantics we want here (the request
 * is well-formed, but conflicts with the resource's current state).
 */
public class InvalidStatusTransitionException extends IllegalStateException {

    public InvalidStatusTransitionException(LoanStatus current, LoanStatus target) {
        super("Cannot move loan from status '" + current + "' to '" + target + "'. "
                + "Allowed next statuses: " + current.nextAllowed());
    }
}
