package com.dpcoutreach.backend.dto;

import com.dpcoutreach.backend.entity.Dpc;

public record DpcResponse(Long id, String name) {

    public static DpcResponse from(Dpc dpc) {
        return new DpcResponse(dpc.getId(), dpc.getName());
    }
}
