package com.nexus.chat.contract.exception;

/**
 * Thrown when a client session has timed out or is unrecognized by the server.
 */
public class SessionExpiredException extends NexusChatException {
    public SessionExpiredException(String message) {
        super("SESSION_EXPIRED", message);
    }
}
