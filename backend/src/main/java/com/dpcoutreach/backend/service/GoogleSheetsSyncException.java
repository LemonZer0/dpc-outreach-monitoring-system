package com.dpcoutreach.backend.service;

public class GoogleSheetsSyncException extends RuntimeException {
    public GoogleSheetsSyncException(String message, Throwable cause) {
        super(message, cause);
    }
}
