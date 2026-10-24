package com.dpcoutreach.backend.controller;

import com.dpcoutreach.backend.dto.DpcResponse;
import com.dpcoutreach.backend.service.DpcQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dpcs")
public class DpcController {

    private final DpcQueryService dpcQueryService;

    public DpcController(DpcQueryService dpcQueryService) {
        this.dpcQueryService = dpcQueryService;
    }

    @GetMapping
    public List<DpcResponse> getDpcs() {
        return dpcQueryService.findAll();
    }
}
