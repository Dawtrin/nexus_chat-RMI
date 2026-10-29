package com.nexus.chat.contract.exception;

/**
 * Thrown when administrative authentication or user validation fails.
 */
public class AuthenticationException extends NexusChatException {
    public AuthenticationException(String message) {
        super("AUTH_FAILED", message);
    }
}
