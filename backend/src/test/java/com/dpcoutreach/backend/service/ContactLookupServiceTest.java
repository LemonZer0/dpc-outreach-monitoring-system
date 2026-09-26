package com.dpcoutreach.backend.service;

import com.dpcoutreach.backend.entity.Contact;
import com.dpcoutreach.backend.repository.ContactRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContactLookupServiceTest {

    @Test
    void normalizesNumberBeforeRepositoryLookup() {
        ContactRepository contactRepository = mock(ContactRepository.class);
        List<Contact> contacts = List.of();
        when(contactRepository.findAllByNormalizedPhoneNumber("919876543210")).thenReturn(contacts);

        List<Contact> result =
                new ContactLookupService(contactRepository).findMatchesByPhoneNumber("+91 (98765) 432-10");

        assertSame(contacts, result);
        verify(contactRepository).findAllByNormalizedPhoneNumber("919876543210");
    }
}
