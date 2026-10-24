package com.dpcoutreach.backend.controller;

import com.dpcoutreach.backend.dto.ContactResponse;
import com.dpcoutreach.backend.service.ContactQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/contacts")
public class ContactController {

    private final ContactQueryService contactQueryService;

    public ContactController(ContactQueryService contactQueryService) {
        this.contactQueryService = contactQueryService;
    }

    @GetMapping
    public List<ContactResponse> getContacts() {
        return contactQueryService.findAll();
    }
}
