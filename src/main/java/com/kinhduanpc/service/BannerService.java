package com.kinhduanpc.service;

import com.kinhduanpc.dto.BannerDTO;
import com.kinhduanpc.dto.BannerRequest;
import com.kinhduanpc.entity.Banner;
import com.kinhduanpc.exception.AppException;
import com.kinhduanpc.repository.BannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BannerService {

    private final BannerRepository bannerRepo;

    @Transactional(readOnly = true)
    public List<BannerDTO> getActiveBanners(String position) {
        return bannerRepo.findActiveBannersByPosition(position, LocalDateTime.now())
            .stream()
            .map(this::toDTO)
            .toList();
    }

    public BannerDTO create(BannerRequest request) {
        Banner banner = Banner.builder()
            .title(request.getTitle())
            .imageUrl(request.getImageUrl())
            .linkUrl(request.getLinkUrl())
            .position(request.getPosition())
            .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
            .startDate(request.getStartDate())
            .endDate(request.getEndDate())
            .isActive(request.getIsActive() != null ? request.getIsActive() : true)
            .build();

        Banner saved = bannerRepo.save(banner);
        return toDTO(saved);
    }

    public BannerDTO update(Long id, BannerRequest request) {
        Banner banner = bannerRepo.findById(id)
            .orElseThrow(() -> AppException.notFound("Banner"));

        banner.setTitle(request.getTitle());
        banner.setImageUrl(request.getImageUrl());
        banner.setLinkUrl(request.getLinkUrl());
        banner.setPosition(request.getPosition());
        
        if (request.getSortOrder() != null) {
            banner.setSortOrder(request.getSortOrder());
        }
        if (request.getStartDate() != null) {
            banner.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            banner.setEndDate(request.getEndDate());
        }
        if (request.getIsActive() != null) {
            banner.setIsActive(request.getIsActive());
        }

        Banner updated = bannerRepo.save(banner);
        return toDTO(updated);
    }

    public void delete(Long id) {
        if (!bannerRepo.existsById(id)) {
            throw AppException.notFound("Banner");
        }
        bannerRepo.deleteById(id);
    }

    // Mapping method
    private BannerDTO toDTO(Banner banner) {
        return BannerDTO.builder()
            .id(banner.getId())
            .title(banner.getTitle())
            .imageUrl(banner.getImageUrl())
            .linkUrl(banner.getLinkUrl())
            .position(banner.getPosition())
            .sortOrder(banner.getSortOrder())
            .startDate(banner.getStartDate())
            .endDate(banner.getEndDate())
            .isActive(banner.getIsActive())
            .build();
    }
}
