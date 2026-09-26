package com.dpcoutreach.backend.repository;

import com.dpcoutreach.backend.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContactRepository extends JpaRepository<Contact, Long> {

    @Query(value = "SELECT c.* FROM contacts c "
            + "WHERE regexp_replace(c.phone_number, '[^0-9]', '', 'g') = :normalizedPhoneNumber",
            nativeQuery = true)
    List<Contact> findAllByNormalizedPhoneNumber(@Param("normalizedPhoneNumber") String normalizedPhoneNumber);
}
