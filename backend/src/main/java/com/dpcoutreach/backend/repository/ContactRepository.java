package com.dpcoutreach.backend.repository;

import com.dpcoutreach.backend.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ContactRepository extends JpaRepository<Contact, Long> {

    @Query("SELECT c FROM Contact c JOIN FETCH c.company ORDER BY c.company.name, c.name")
    List<Contact> findAllWithCompanyOrderByCompanyNameAndName();

    List<Contact> findAllByCompany_Id(Long companyId);

    @Query(value = "SELECT c.* FROM contacts c "
            + "WHERE regexp_replace(c.phone_number, '[^0-9]', '', 'g') = :normalizedPhoneNumber",
            nativeQuery = true)
    List<Contact> findAllByNormalizedPhoneNumber(@Param("normalizedPhoneNumber") String normalizedPhoneNumber);

    Optional<Contact> findFirstByPhoneNumberAndCompanyId(String phoneNumber, Long companyId);
}
