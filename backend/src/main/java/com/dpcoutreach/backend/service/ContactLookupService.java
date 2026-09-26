package com.dpcoutreach.backend.service;

import com.dpcoutreach.backend.entity.Contact;
import com.dpcoutreach.backend.repository.ContactRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContactLookupService {

    private final ContactRepository contactRepository;

    public ContactLookupService(ContactRepository contactRepository) {
        this.contactRepository = contactRepository;
    }

    public List<Contact> findMatchesByPhoneNumber(String phoneNumber) {
        String normalizedPhoneNumber = PhoneNumberNormalizer.normalize(phoneNumber);
        return contactRepository.findAllByNormalizedPhoneNumber(normalizedPhoneNumber);
    }
}
