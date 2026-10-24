package com.dpcoutreach.backend.service;

import com.dpcoutreach.backend.dto.DpcResponse;
import com.dpcoutreach.backend.repository.DpcRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DpcQueryService {

    private final DpcRepository dpcRepository;

    public DpcQueryService(DpcRepository dpcRepository) {
        this.dpcRepository = dpcRepository;
    }

    @Transactional(readOnly = true)
    public List<DpcResponse> findAll() {
        return dpcRepository.findAllByOrderByNameAsc().stream()
                .map(DpcResponse::from)
                .toList();
    }
}
