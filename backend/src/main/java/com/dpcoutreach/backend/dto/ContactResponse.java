package com.dpcoutreach.backend.dto;

import com.dpcoutreach.backend.entity.Contact;

public record ContactResponse(
        Long id,
        String name,
        String phoneNumber,
        Long companyId,
        String companyName) {

    public static ContactResponse from(Contact contact) {
        return new ContactResponse(
                contact.getId(),
                contact.getName(),
                contact.getPhoneNumber(),
                contact.getCompany().getId(),
                contact.getCompany().getName());
    }
}
