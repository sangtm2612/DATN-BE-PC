package com.kinhduanpc.controller;

import com.kinhduanpc.dto.ApiResponse;
import com.kinhduanpc.entity.PcComponentType;
import com.kinhduanpc.repository.PcComponentTypeRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/build-pc")
@RequiredArgsConstructor
@Tag(name = "Build PC", description = "Tính năng Build PC")
public class BuildPcController {

    private final PcComponentTypeRepository componentTypeRepo;

    @GetMapping("/component-types")
    public ResponseEntity<ApiResponse<List<PcComponentType>>> getComponentTypes() {
        return ResponseEntity.ok(ApiResponse.success(
            componentTypeRepo.findAllByOrderBySortOrderAsc()
        ));
    }
}
