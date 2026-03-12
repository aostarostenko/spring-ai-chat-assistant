package com.nexus.chatassistant.domain.exception;

/**
 * Exception for security-related failures (Authorization, Authentication, CSRF).
 * Allows the system to handle security threats differently than business errors.
 */
public class SecurityException extends RuntimeException {
    private final String securityCode;

    public SecurityException(String message, String securityCode) {
        super(message);
        this.securityCode = securityCode;
    }

    public String getSecurityCode() {
        return securityCode;
    }
}