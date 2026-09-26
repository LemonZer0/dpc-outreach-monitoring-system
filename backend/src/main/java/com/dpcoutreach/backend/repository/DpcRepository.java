package com.dpcoutreach.backend.repository;

import com.dpcoutreach.backend.entity.Dpc;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DpcRepository extends JpaRepository<Dpc, Long> {
}