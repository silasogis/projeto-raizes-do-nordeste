package com.raizesdonordeste.domain.exceptions;

import java.util.Collections;
import java.util.List;

public class BusinessValidationException extends RuntimeException {

    private final List<String> details;

    public BusinessValidationException(String message) {
        super(message);
        this.details = Collections.emptyList();
    }

    public BusinessValidationException(String message, List<String> details) {
        super(message);
        this.details = details != null ? details : Collections.emptyList();
    }

    public List<String> getDetails() {
        return details;
    }
}
