
package com.dpcoutreach.backend.repository;

import com.dpcoutreach.backend.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findFirstByName(String name);
}
