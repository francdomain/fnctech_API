package com.fintech.exception;

public class FintechException extends RuntimeException {
    public FintechException(String message) {
        super(message);
    }
    public FintechException(String message, Throwable cause) {
        super(message, cause);
    }
}
