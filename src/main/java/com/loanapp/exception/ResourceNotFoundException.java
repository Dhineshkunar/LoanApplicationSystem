package com.loanapp.exception;

/**
 * Thrown when a requested entity does not exist.
 * Mapped to HTTP 404 in GlobalExceptionHandler.
 *
 * A custom unchecked exception (extends RuntimeException) is preferred
 * over throwing a generic RuntimeException with a message: the exception
 * TYPE itself carries meaning, so @ExceptionHandler can dispatch on it,
 * and callers reading service code immediately understand the failure
 * mode without reading the message string.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException forLoan(Long id) {
        return new ResourceNotFoundException("Loan not found with id " + id);
    }
}
