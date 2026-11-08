package com.dpcoutreach.backend.service;

import com.dpcoutreach.backend.entity.Dpc;
import com.dpcoutreach.backend.repository.DpcRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DpcCreationServiceTest {

    private final EntityManager entityManager = mock(EntityManager.class);
    private final DpcRepository dpcRepository = mock(DpcRepository.class);
    private final DpcCreationService service = new DpcCreationService(entityManager, dpcRepository);

    @Test
    void allocatesOneWhenNoDpcsExist() {
        Query lockQuery = mock(Query.class);
        when(entityManager.createNativeQuery("LOCK TABLE dpcs IN EXCLUSIVE MODE")).thenReturn(lockQuery);
        when(dpcRepository.findAllIdsByOrderByIdAsc()).thenReturn(List.of());

        Dpc dpc = service.create("New DPC", null);

        assertEquals(1L, dpc.getId());
        assertEquals("New DPC", dpc.getName());
        verify(lockQuery).executeUpdate();
        verify(entityManager).persist(dpc);
    }

    @Test
    void allocatesLowestMissingId() {
        Query lockQuery = mock(Query.class);
        when(entityManager.createNativeQuery("LOCK TABLE dpcs IN EXCLUSIVE MODE")).thenReturn(lockQuery);
        when(dpcRepository.findAllIdsByOrderByIdAsc()).thenReturn(List.of(1L, 2L, 4L, 5L));

        Dpc dpc = service.create("New DPC", null);

        assertEquals(3L, dpc.getId());
        verify(lockQuery).executeUpdate();
        verify(entityManager).persist(dpc);
    }
}
