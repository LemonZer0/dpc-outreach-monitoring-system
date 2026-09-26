
package com.dpcoutreach.backend.repository;

import com.dpcoutreach.backend.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {
}
    