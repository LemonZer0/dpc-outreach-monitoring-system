package com.dpcoutreach.backend.service;

public class AdminResourceNotFoundException extends RuntimeException {

    public AdminResourceNotFoundException(String resource, Long id) {
        super(resource + " with id " + id + " was not found");
    }
}
