package com.kinhduanpc.controller;

import com.kinhduanpc.dto.ApiResponse;
import com.kinhduanpc.dto.pcbuild.CompatibilityCheckRequest;
import com.kinhduanpc.dto.pcbuild.CompatibilityCheckResponse;
import com.kinhduanpc.dto.pcbuild.PcBuildRequest;
import com.kinhduanpc.dto.pcbuild.PcBuildResponse;
import com.kinhduanpc.entity.PcComponent;
import com.kinhduanpc.entity.PcComponentType;
import com.kinhduanpc.repository.PcComponentRepository;
import com.kinhduanpc.repository.PcComponentTypeRepository;
import com.kinhduanpc.service.PcBuildService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/build-pc")
@RequiredArgsConstructor
@Tag(name = "Build PC", description = "Tính năng Build PC")
public class BuildPcController {

    private final PcComponentTypeRepository componentTypeRepo;
    private final PcComponentRepository componentRepo;
    private final PcBuildService pcBuildService;

    @GetMapping("/component-types")
    public ResponseEntity<ApiResponse<List<PcComponentType>>> getComponentTypes() {
        return ResponseEntity.ok(ApiResponse.success(
            componentTypeRepo.findAllByOrderBySortOrderAsc()
        ));
    }

    @GetMapping("/components")
    public ResponseEntity<ApiResponse<List<PcComponent>>> getComponents(@RequestParam Long typeId) {
        return ResponseEntity.ok(ApiResponse.success(componentRepo.findByComponentTypeId(typeId)));
    }

    @PostMapping("/check-compatibility")
    public ResponseEntity<ApiResponse<CompatibilityCheckResponse>> checkCompatibility(
            @RequestBody CompatibilityCheckRequest req) {
        return ResponseEntity.ok(ApiResponse.success(pcBuildService.checkCompatibility(req.getProductIds())));
    }

    @PostMapping("/builds")
    public ResponseEntity<ApiResponse<PcBuildResponse>> saveBuild(
            Authentication auth,
            @Valid @RequestBody PcBuildRequest req) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(pcBuildService.saveBuild(userId, req), "Đã lưu cấu hình"));
    }

    @GetMapping("/builds")
    public ResponseEntity<ApiResponse<List<PcBuildResponse>>> getMyBuilds(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(pcBuildService.getUserBuilds(userId)));
    }

    @GetMapping("/builds/{id}")
    public ResponseEntity<ApiResponse<PcBuildResponse>> getBuildDetail(
            @PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(pcBuildService.getBuildDetail(userId, id)));
    }

    @DeleteMapping("/builds/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBuild(
            @PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        pcBuildService.deleteBuild(userId, id);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xóa cấu hình"));
    }
}
