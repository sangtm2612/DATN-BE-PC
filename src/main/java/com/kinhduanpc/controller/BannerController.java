package com.kinhduanpc.controller;

import com.kinhduanpc.dto.ApiResponse;
import com.kinhduanpc.entity.Banner;
import com.kinhduanpc.repository.BannerRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/banners")
@RequiredArgsConstructor
@Tag(name = "Banners", description = "Banner & Slider")
public class BannerController {

    private final BannerRepository bannerRepo;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Banner>>> getByPosition(
            @RequestParam(defaultValue = "home_slider") String position) {
        return ResponseEntity.ok(ApiResponse.success(
            bannerRepo.findActiveBannersByPosition(position, LocalDateTime.now())));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Banner>> create(@RequestBody Banner banner) {
        return ResponseEntity.ok(ApiResponse.success(bannerRepo.save(banner)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Banner>> update(@PathVariable Long id, @RequestBody Banner req) {
        Banner b = bannerRepo.findById(id).orElseThrow();
        b.setTitle(req.getTitle()); b.setImageUrl(req.getImageUrl());
        b.setLinkUrl(req.getLinkUrl()); b.setIsActive(req.getIsActive());
        b.setSortOrder(req.getSortOrder());
        return ResponseEntity.ok(ApiResponse.success(bannerRepo.save(b)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        bannerRepo.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xóa banner"));
    }
}
