package com.nexus.chatassistant.domain.exception;

/**
 * Exception specifically for data access and persistence layers.
 * Separates database-level errors from web/business logic errors.
 */
public class DaoException extends RuntimeException {
    private final String errorCode;

    public DaoException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}