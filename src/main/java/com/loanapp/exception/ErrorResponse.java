package com.loanapp.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Uniform error payload returned by every handler in
 * GlobalExceptionHandler. A single, predictable shape means frontend/API
 * consumers write ONE error-parsing code path instead of one per endpoint.
 *
 * @JsonInclude(NON_NULL) means the optional "validationErrors" map is
 * simply omitted from the JSON for non-validation errors, instead of
 * serializing as "validationErrors": null.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    private int status;
    private String error;
    private String message;
    private String path;

    /** Populated only for 400s coming from Bean Validation failures. */
    private Map<String, String> validationErrors;
}
