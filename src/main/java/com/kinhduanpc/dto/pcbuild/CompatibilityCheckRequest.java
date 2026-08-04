package com.kinhduanpc.dto.pcbuild;

import lombok.Data;

import java.util.List;

@Data
public class CompatibilityCheckRequest {
    private List<Long> productIds;
}
