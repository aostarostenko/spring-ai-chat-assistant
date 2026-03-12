package com.nexus.chatassistant.domain.exception;

/**
 * Centralized repository for all system error codes.
 */
public final class ErrorCodes {
    // Security Errors
    public static final String AUTH_FAILED = "SEC_001";
    public static final String ACCESS_DENIED = "SEC_002";
    public static final String USER_NOT_FOUND = "SEC_003";

    // Business/Web Errors
    public static final String DUPLICATE_USER = "WEB_101";
    public static final String INVALID_INPUT = "WEB_102";
    public static final String AI_SERVICE_ERROR = "WEB_103";

    // Data Access (DAO) Errors
    public static final String DB_CONNECTION_FAILURE = "DAO_001";
    public static final String DB_WRITE_FAILURE = "DAO_002";
    public static final String DB_READ_FAILURE = "DAO_003";
    public static final String DB_CONSTRAINT_VIOLATION = "DAO_004";
    public static final String PERSISTENCE_FAILED = "DAO_005";

    public static final String SESSION_INVALID = "SEC_004";
    public static final String CREDENTIAL_MISMATCH = "SEC_005";
    public static final String UNAUTHORIZED = "SEC_006";
    public static final String SESSION_ACCESS_DENIED = "SEC_007";

    private ErrorCodes() {} // Prevent instantiation
}