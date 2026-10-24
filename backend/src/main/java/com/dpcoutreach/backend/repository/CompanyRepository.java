
package com.dpcoutreach.backend.repository;

import com.dpcoutreach.backend.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    List<Company> findAllByOrderByNameAsc();

    Optional<Company> findFirstByName(String name);
}
