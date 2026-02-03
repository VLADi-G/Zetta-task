package com.zetta.task.exception;

public class ConditionException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public ConditionException(String message) {
        super(message);
    }

    public ConditionException(String message, Throwable cause) {
        super(message, cause);
    }
}
