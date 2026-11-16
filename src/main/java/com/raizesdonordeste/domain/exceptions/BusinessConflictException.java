package com.raizesdonordeste.domain.exceptions;

public class BusinessConflictException extends RuntimeException {

    private final String errorCode;

    public BusinessConflictException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessConflictException(String message) {
        super(message);
        this.errorCode = "CONFLITO_NEGOCIO";
    }

    public String getErrorCode() {
        return errorCode;
    }
}
