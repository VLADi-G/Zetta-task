package com.zetta.task.exception;

public class ConditionException extends RuntimeException {
    public ConditionException(String message) {
        super(message);
    }

    public ConditionException(String message, Throwable cause) {
        super(message, cause);
    }
}
