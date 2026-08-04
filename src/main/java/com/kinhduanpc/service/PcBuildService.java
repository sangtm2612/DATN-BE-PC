package com.kinhduanpc.service;

import com.kinhduanpc.dto.pcbuild.CompatibilityCheckResponse;
import com.kinhduanpc.dto.pcbuild.PcBuildRequest;
import com.kinhduanpc.dto.pcbuild.PcBuildResponse;
import com.kinhduanpc.entity.PcBuild;
import com.kinhduanpc.entity.PcBuildItem;
import com.kinhduanpc.entity.PcComponent;
import com.kinhduanpc.entity.PcComponentType;
import com.kinhduanpc.entity.Product;
import com.kinhduanpc.exception.AppException;
import com.kinhduanpc.repository.OrderRepository;
import com.kinhduanpc.repository.PcBuildRepository;
import com.kinhduanpc.repository.PcComponentRepository;
import com.kinhduanpc.repository.PcComponentTypeRepository;
import com.kinhduanpc.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class PcBuildService {

    private final PcComponentRepository pcComponentRepo;
    private final PcComponentTypeRepository pcComponentTypeRepo;
    private final PcBuildRepository pcBuildRepo;
    private final ProductRepository productRepo;
    private final OrderRepository orderRepo;

    public CompatibilityCheckResponse checkCompatibility(List<Long> productIds) {
        List<String> issues = new ArrayList<>();
        boolean compatible = true;

        if (productIds == null || productIds.isEmpty()) {
            return CompatibilityCheckResponse.builder().isCompatible(true).issues(issues).build();
        }

        Map<String, PcComponent> bySlug = new HashMap<>();
        for (PcComponent c : pcComponentRepo.findByProductIdIn(productIds)) {
            bySlug.put(c.getComponentType().getSlug(), c);
        }

        PcComponent cpu = bySlug.get("cpu");
        PcComponent mainboard = bySlug.get("mainboard");
        PcComponent ram = bySlug.get("ram");
        PcComponent vga = bySlug.get("vga");
        PcComponent psu = bySlug.get("psu");

        if (cpu != null && mainboard != null
                && cpu.getSocket() != null && mainboard.getSocket() != null
                && !cpu.getSocket().equalsIgnoreCase(mainboard.getSocket())) {
            issues.add("CPU (socket " + cpu.getSocket() + ") không tương thích với Mainboard (socket " + mainboard.getSocket() + ")");
            compatible = false;
        }

        if (mainboard != null && ram != null
                && mainboard.getRamType() != null && ram.getRamType() != null
                && !mainboard.getRamType().equalsIgnoreCase(ram.getRamType())) {
            issues.add("RAM (" + ram.getRamType() + ") không tương thích với Mainboard (hỗ trợ " + mainboard.getRamType() + ")");
            compatible = false;
        }

        int totalTdp = (cpu != null && cpu.getTdpWatts() != null ? cpu.getTdpWatts() : 0)
            + (vga != null && vga.getTdpWatts() != null ? vga.getTdpWatts() : 0);
        if (totalTdp > 0) {
            if (psu == null) {
                issues.add("Chưa chọn nguồn (PSU) — tổng công suất ước tính " + totalTdp + "W");
            } else if (psu.getPsuWattage() != null && totalTdp > psu.getPsuWattage()) {
                issues.add("Nguồn (PSU " + psu.getPsuWattage() + "W) không đủ công suất cho tổng TDP ước tính " + totalTdp + "W");
                compatible = false;
            }
        }

        return CompatibilityCheckResponse.builder().isCompatible(compatible).issues(issues).build();
    }

    public PcBuildResponse saveBuild(Long userId, PcBuildRequest req) {
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw AppException.badRequest("EMPTY_BUILD", "Cấu hình chưa có linh kiện nào");
        }

        PcBuild build = PcBuild.builder()
            .userId(userId)
            .name(req.getName() != null && !req.getName().isBlank() ? req.getName() : "Cấu hình PC của tôi")
            .description(req.getDescription())
            .build();

        BigDecimal total = BigDecimal.ZERO;
        for (PcBuildRequest.Item item : req.getItems()) {
            Product product = productRepo.findById(item.getProductId())
                .orElseThrow(() -> AppException.notFound("Sản phẩm"));
            PcComponentType type = pcComponentTypeRepo.findById(item.getComponentTypeId())
                .orElseThrow(() -> AppException.notFound("Loại linh kiện"));
            int qty = item.getQuantity() != null && item.getQuantity() > 0 ? item.getQuantity() : 1;

            build.getItems().add(PcBuildItem.builder()
                .build(build).componentType(type).product(product)
                .quantity(qty).unitPrice(product.getPrice())
                .build());
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(qty)));
        }
        build.setTotalPrice(total);

        pcBuildRepo.save(build);
        return toResponse(build);
    }

    public List<PcBuildResponse> getUserBuilds(Long userId) {
        return pcBuildRepo.findByUserId(userId).stream().map(this::toResponse).toList();
    }

    public PcBuildResponse getBuildDetail(Long userId, Long buildId) {
        return toResponse(findOwnedBuild(userId, buildId));
    }

    public void deleteBuild(Long userId, Long buildId) {
        PcBuild build = findOwnedBuild(userId, buildId);
        if (orderRepo.existsByBuildId(buildId)) {
            throw AppException.conflict("BUILD_HAS_ORDERS",
                "Không thể xóa cấu hình đã dùng để đặt hàng");
        }
        pcBuildRepo.delete(build);
    }

    private PcBuild findOwnedBuild(Long userId, Long buildId) {
        PcBuild build = pcBuildRepo.findById(buildId)
            .orElseThrow(() -> AppException.notFound("Cấu hình PC"));
        if (!build.getUserId().equals(userId)) {
            throw AppException.forbidden("Bạn không có quyền truy cập cấu hình này");
        }
        return build;
    }

    private PcBuildResponse toResponse(PcBuild build) {
        return PcBuildResponse.builder()
            .id(build.getId()).name(build.getName()).description(build.getDescription())
            .totalPrice(build.getTotalPrice()).createdAt(build.getCreatedAt())
            .items(build.getItems().stream().map(i -> PcBuildResponse.ItemResponse.builder()
                .id(i.getId())
                .componentTypeId(i.getComponentType().getId())
                .componentTypeName(i.getComponentType().getName())
                .productId(i.getProduct().getId())
                .productName(i.getProduct().getName())
                .productThumbnail(i.getProduct().getThumbnail())
                .quantity(i.getQuantity())
                .unitPrice(i.getUnitPrice())
                .build()).toList())
            .build();
    }
}
