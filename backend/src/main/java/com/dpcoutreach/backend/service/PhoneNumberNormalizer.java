package com.dpcoutreach.backend.service;

public final class PhoneNumberNormalizer {

    private PhoneNumberNormalizer() {
    }

    public static String normalize(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new IllegalArgumentException("Phone number must not be blank");
        }

        if (!phoneNumber.matches("\\+?[0-9\\s().-]+")) {
            throw new IllegalArgumentException("Phone number contains unsupported characters");
        }

        String digitsOnly = phoneNumber.replaceAll("\\D", "");
        if (digitsOnly.isEmpty()) {
            throw new IllegalArgumentException("Phone number must contain digits");
        }

        return digitsOnly;
    }
}
