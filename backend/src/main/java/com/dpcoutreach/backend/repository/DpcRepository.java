package com.dpcoutreach.backend.repository;

import com.dpcoutreach.backend.entity.Dpc;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DpcRepository extends JpaRepository<Dpc, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select dpc from Dpc dpc where dpc.id = :dpcId")
    Optional<Dpc> findByIdForUpdate(@Param("dpcId") Long dpcId);

    Optional<Dpc> findFirstByName(String name);
}
