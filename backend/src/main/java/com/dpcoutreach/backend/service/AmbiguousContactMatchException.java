package com.dpcoutreach.backend.service;

public class AmbiguousContactMatchException extends RuntimeException {

    public AmbiguousContactMatchException(String normalizedPhoneNumber) {
        super("Phone number matches multiple contacts: " + normalizedPhoneNumber);
    }
}
