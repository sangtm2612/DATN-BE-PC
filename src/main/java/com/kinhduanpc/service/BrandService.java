package com.kinhduanpc.service;

import com.kinhduanpc.dto.BrandDTO;
import com.kinhduanpc.dto.BrandRequest;
import com.kinhduanpc.entity.Brand;
import com.kinhduanpc.exception.AppException;
import com.kinhduanpc.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BrandService {

    private final BrandRepository brandRepo;

    @Transactional(readOnly = true)
    public List<BrandDTO> findAll() {
        return brandRepo.findByIsActiveTrueOrderByNameAsc().stream()
            .map(this::toDTO)
            .toList();
    }

    @Transactional(readOnly = true)
    public BrandDTO findBySlug(String slug) {
        Brand brand = brandRepo.findBySlug(slug)
            .orElseThrow(() -> AppException.notFound("Thương hiệu"));
        return toDTO(brand);
    }

    public BrandDTO create(BrandRequest request) {
        String slug = generateSlug(request.getName());
        
        if (brandRepo.findBySlug(slug).isPresent()) {
            throw AppException.conflict("BRAND_EXISTS", "Thương hiệu với tên này đã tồn tại");
        }

        Brand brand = Brand.builder()
            .name(request.getName())
            .slug(slug)
            .logoUrl(request.getLogoUrl())
            .website(request.getWebsite())
            .description(request.getDescription())
            .isActive(request.getIsActive() != null ? request.getIsActive() : true)
            .build();

        Brand saved = brandRepo.save(brand);
        return toDTO(saved);
    }

    public BrandDTO update(Long id, BrandRequest request) {
        Brand brand = brandRepo.findById(id)
            .orElseThrow(() -> AppException.notFound("Thương hiệu"));

        brand.setName(request.getName());
        brand.setSlug(generateSlug(request.getName()));
        brand.setLogoUrl(request.getLogoUrl());
        brand.setWebsite(request.getWebsite());
        brand.setDescription(request.getDescription());
        if (request.getIsActive() != null) {
            brand.setIsActive(request.getIsActive());
        }

        Brand updated = brandRepo.save(brand);
        return toDTO(updated);
    }

    public void delete(Long id) {
        if (!brandRepo.existsById(id)) {
            throw AppException.notFound("Thương hiệu");
        }
        brandRepo.deleteById(id);
    }

    // Mapping method
    private BrandDTO toDTO(Brand brand) {
        return BrandDTO.builder()
            .id(brand.getId())
            .name(brand.getName())
            .slug(brand.getSlug())
            .logoUrl(brand.getLogoUrl())
            .website(brand.getWebsite())
            .description(brand.getDescription())
            .isActive(brand.getIsActive())
            .build();
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
            .replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a")
            .replaceAll("[èéẹẻẽêềếệểễ]", "e")
            .replaceAll("[ìíịỉĩ]", "i")
            .replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o")
            .replaceAll("[ùúụủũưừứựửữ]", "u")
            .replaceAll("[ỳýỵỷỹ]", "y")
            .replaceAll("[đ]", "d")
            .replaceAll("[^a-z0-9\\s-]", "")
            .replaceAll("\\s+", "-")
            .replaceAll("-+", "-")
            .trim();
    }
}
