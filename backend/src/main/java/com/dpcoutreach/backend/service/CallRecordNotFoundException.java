package com.dpcoutreach.backend.service;

public class CallRecordNotFoundException extends RuntimeException {

    public CallRecordNotFoundException(Long id) {
        super("Call record with id " + id + " was not found");
    }
}
