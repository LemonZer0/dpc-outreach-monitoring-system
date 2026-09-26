package com.dpcoutreach.backend.repository;

import com.dpcoutreach.backend.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContactRepository extends JpaRepository<Contact, Long> {

    List<Contact> findAllByPhoneNumber(String phoneNumber);
}