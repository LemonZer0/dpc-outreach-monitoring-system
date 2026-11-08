package com.dpcoutreach.backend.service;

import com.dpcoutreach.backend.entity.Dpc;
import com.dpcoutreach.backend.repository.DpcRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DpcCreationService {

    private final EntityManager entityManager;
    private final DpcRepository dpcRepository;

    public DpcCreationService(EntityManager entityManager, DpcRepository dpcRepository) {
        this.entityManager = entityManager;
        this.dpcRepository = dpcRepository;
    }

    @Transactional
    public Dpc create(String name, String accountInformation) {
        entityManager.createNativeQuery("LOCK TABLE dpcs IN EXCLUSIVE MODE").executeUpdate();
        long nextId = 1;
        for (Long existingId : dpcRepository.findAllIdsByOrderByIdAsc()) {
            if (existingId > nextId) {
                break;
            }
            if (existingId == nextId) {
                nextId++;
            }
        }

        Dpc dpc = new Dpc(nextId, name, accountInformation);
        entityManager.persist(dpc);
        return dpc;
    }
}
