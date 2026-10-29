package com.nexus.chat.contract.exception;

/**
 * Base checked exception for business and communication errors in NEXUS Chat.
 */
public class NexusChatException extends Exception {
    private final String errorCode;

    public NexusChatException(String message) {
        super(message);
        this.errorCode = "GENERAL_ERROR";
    }

    public NexusChatException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public NexusChatException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
