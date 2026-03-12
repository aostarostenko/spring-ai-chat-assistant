package com.nexus.chatassistant.domain.exception;

/**
 * Unified exception for all web-facing business logic errors.
 * Replaces specific exceptions like ResourceNotFoundException.
 */
public class WebException extends RuntimeException {
    private final String errorCode;

    public WebException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}