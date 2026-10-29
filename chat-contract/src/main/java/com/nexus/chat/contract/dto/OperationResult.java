package com.nexus.chat.contract.dto;

import java.io.Serial;
import java.io.Serializable;

/**
 * Standard operation response container crossing RMI boundaries.
 *
 * @param <T> Payload type
 */
public class OperationResult<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final boolean success;
    private final String message;
    private final String errorCode;
    private final T data;

    public OperationResult(boolean success, String message, String errorCode, T data) {
        this.success = success;
        this.message = message != null ? message : "";
        this.errorCode = errorCode != null ? errorCode : "";
        this.data = data;
    }

    public static <T> OperationResult<T> success(T data) {
        return new OperationResult<>(true, "Operation successful", null, data);
    }

    public static <T> OperationResult<T> success(String message, T data) {
        return new OperationResult<>(true, message, null, data);
    }

    public static OperationResult<Void> ok() {
        return new OperationResult<>(true, "Operation successful", null, null);
    }

    public static OperationResult<Void> ok(String message) {
        return new OperationResult<>(true, message, null, null);
    }

    public static <T> OperationResult<T> failure(String errorCode, String message) {
        return new OperationResult<>(false, message, errorCode, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public T getData() {
        return data;
    }

    @Override
    public String toString() {
        return "OperationResult{" +
                "success=" + success +
                ", message='" + message + '\'' +
                (errorCode != null && !errorCode.isEmpty() ? ", errorCode='" + errorCode + '\'' : "") +
                (data != null ? ", data=" + data : "") +
                '}';
    }
}
