package com.dpcoutreach.backend.service;

import com.dpcoutreach.backend.dto.ContactResponse;
import com.dpcoutreach.backend.repository.ContactRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ContactQueryService {

    private final ContactRepository contactRepository;

    public ContactQueryService(ContactRepository contactRepository) {
        this.contactRepository = contactRepository;
    }

    @Transactional(readOnly = true)
    public List<ContactResponse> findAll() {
        return contactRepository.findAllWithCompanyOrderByCompanyNameAndName().stream()
                .map(ContactResponse::from)
                .toList();
    }
}
