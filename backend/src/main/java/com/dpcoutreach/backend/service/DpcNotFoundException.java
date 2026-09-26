package com.dpcoutreach.backend.service;

public class DpcNotFoundException extends RuntimeException {

    public DpcNotFoundException(Long dpcId) {
        super("DPC with id " + dpcId + " was not found");
    }
}
