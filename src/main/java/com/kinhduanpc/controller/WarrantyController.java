package com.kinhduanpc.controller;

import com.kinhduanpc.dto.ApiResponse;
import com.kinhduanpc.dto.warranty.ServiceRequestRequest;
import com.kinhduanpc.dto.warranty.ServiceRequestResponse;
import com.kinhduanpc.dto.warranty.ServiceRequestStatusUpdateRequest;
import com.kinhduanpc.entity.ServiceMedia;
import com.kinhduanpc.entity.ServiceRequest;
import com.kinhduanpc.entity.User;
import com.kinhduanpc.entity.Warranty;
import com.kinhduanpc.exception.AppException;
import com.kinhduanpc.repository.ServiceMediaRepository;
import com.kinhduanpc.repository.ServiceRequestRepository;
import com.kinhduanpc.repository.UserRepository;
import com.kinhduanpc.repository.WarrantyRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/warranties")
@RequiredArgsConstructor
@Tag(name = "Warranty", description = "Bảo hành & Yêu cầu sửa chữa")
public class WarrantyController {

    private final WarrantyRepository warrantyRepo;
    private final ServiceRequestRepository serviceRequestRepo;
    private final ServiceMediaRepository serviceMediaRepo;
    private final UserRepository userRepo;

    private static final AtomicInteger sequence = new AtomicInteger(1);

    private static final java.util.Set<ServiceRequest.ServiceStatus> POST_DIAGNOSIS_STATUSES = java.util.Set.of(
        ServiceRequest.ServiceStatus.repairing,
        ServiceRequest.ServiceStatus.waiting_part,
        ServiceRequest.ServiceStatus.done,
        ServiceRequest.ServiceStatus.returned);

    @GetMapping("/lookup")
    public ResponseEntity<ApiResponse<Warranty>> lookup(
            @RequestParam(required = false) String serial,
            @RequestParam(required = false) String orderCode) {

        if (serial != null) {
            return ResponseEntity.ok(ApiResponse.success(
                warrantyRepo.findBySerialNumber(serial)
                    .orElseThrow(() -> AppException.notFound("Thông tin bảo hành"))));
        }
        if (orderCode != null) {
            List<Warranty> ws = warrantyRepo.findByOrderCode(orderCode);
            if (ws.isEmpty()) throw AppException.notFound("Thông tin bảo hành");
            return ResponseEntity.ok(ApiResponse.success(ws.get(0)));
        }
        throw AppException.badRequest("MISSING_PARAM", "Vui lòng nhập số serial hoặc mã đơn hàng");
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<Warranty>>> getMyWarranties(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(warrantyRepo.findByUserId(userId)));
    }

    // ─── Service Requests (yeu cau sua chua) ───────────────────────────

    @PostMapping("/service-requests")
    public ResponseEntity<ApiResponse<ServiceRequestResponse>> createServiceRequest(
            Authentication auth, @Valid @RequestBody ServiceRequestRequest req) {

        Long userId = (Long) auth.getPrincipal();
        User user = userRepo.findById(userId).orElseThrow(() -> AppException.notFound("Người dùng"));

        Warranty warranty = null;
        String productName = req.getProductName();
        String serialNumber = req.getSerialNumber();

        if (req.getWarrantyId() != null) {
            warranty = warrantyRepo.findById(req.getWarrantyId())
                .orElseThrow(() -> AppException.notFound("Thông tin bảo hành"));
            if (!warranty.getUser().getId().equals(userId)) {
                throw AppException.forbidden("Không có quyền sử dụng bảo hành này");
            }
            productName = warranty.getProduct().getName();
            serialNumber = warranty.getSerialNumber();
        } else if (productName == null || productName.isBlank()) {
            throw AppException.badRequest("MISSING_PRODUCT_NAME",
                "Vui lòng chọn bảo hành có sẵn hoặc nhập tên sản phẩm");
        }

        if (req.getMediaUrls() != null && req.getMediaUrls().size() > 5) {
            throw AppException.badRequest("TOO_MANY_MEDIA", "Tối đa 5 file ảnh/video");
        }

        ServiceRequest sr = ServiceRequest.builder()
            .warranty(warranty)
            .user(user)
            .serviceCode(generateServiceCode())
            .productName(productName)
            .serialNumber(serialNumber)
            .issueDesc(req.getIssueDesc())
            .status(ServiceRequest.ServiceStatus.received)
            .build();
        sr = serviceRequestRepo.save(sr);

        if (req.getMediaUrls() != null && !req.getMediaUrls().isEmpty()) {
            List<ServiceMedia> mediaList = new ArrayList<>();
            int i = 0;
            for (String url : req.getMediaUrls()) {
                mediaList.add(ServiceMedia.builder()
                    .serviceRequest(sr).mediaUrl(url).mediaType("image")
                    .uploadedBy("customer").sortOrder(i++).build());
            }
            serviceMediaRepo.saveAll(mediaList);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse.success(toResponse(sr), "Đã gửi yêu cầu sửa chữa: " + sr.getServiceCode()));
    }

    @GetMapping("/service-requests/my")
    public ResponseEntity<ApiResponse<List<ServiceRequestResponse>>> getMyServiceRequests(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        List<ServiceRequest> requests = serviceRequestRepo.findByUserId(userId).stream()
            .sorted(Comparator.comparing(ServiceRequest::getCreatedAt).reversed())
            .toList();
        return ResponseEntity.ok(ApiResponse.success(toResponseList(requests)));
    }

    @GetMapping("/service-requests/admin")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','TECHNICIAN')")
    public ResponseEntity<ApiResponse<List<ServiceRequestResponse>>> getAdminServiceRequests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long storeId) {

        List<ServiceRequest> requests;
        if (status != null) {
            try {
                requests = serviceRequestRepo.findByStatusOrderByCreatedAtDesc(
                    ServiceRequest.ServiceStatus.valueOf(status));
            } catch (IllegalArgumentException e) {
                throw AppException.badRequest("INVALID_STATUS", "Trạng thái không hợp lệ");
            }
        } else {
            requests = serviceRequestRepo.findAllByOrderByCreatedAtDesc();
        }

        if (storeId != null) {
            requests = requests.stream().filter(r -> storeId.equals(r.getStoreId())).toList();
        }

        return ResponseEntity.ok(ApiResponse.success(toResponseList(requests)));
    }

    @GetMapping("/service-requests/technicians")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTechnicians() {
        List<Map<String, Object>> list = userRepo.findByRole(User.UserRole.technician).stream()
            .map(u -> Map.<String, Object>of("id", u.getId(), "fullName", u.getFullName()))
            .toList();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PutMapping("/service-requests/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','TECHNICIAN')")
    public ResponseEntity<ApiResponse<ServiceRequestResponse>> updateServiceRequestStatus(
            @PathVariable Long id, @Valid @RequestBody ServiceRequestStatusUpdateRequest req) {

        ServiceRequest sr = serviceRequestRepo.findById(id)
            .orElseThrow(() -> AppException.notFound("Yêu cầu sửa chữa"));

        ServiceRequest.ServiceStatus newStatus;
        try {
            newStatus = ServiceRequest.ServiceStatus.valueOf(req.getStatus());
        } catch (IllegalArgumentException e) {
            throw AppException.badRequest("INVALID_STATUS", "Trạng thái không hợp lệ");
        }

        BigDecimal effectiveRepairCost = req.getRepairCost() != null ? req.getRepairCost() : sr.getRepairCost();
        if (POST_DIAGNOSIS_STATUSES.contains(newStatus)
                && effectiveRepairCost != null && effectiveRepairCost.compareTo(BigDecimal.ZERO) > 0
                && !Boolean.TRUE.equals(sr.getCustomerApprovedRepair())) {
            throw AppException.badRequest("REPAIR_NOT_APPROVED",
                "Khách hàng chưa duyệt báo giá sửa chữa, chưa thể chuyển sang trạng thái này");
        }

        sr.setStatus(newStatus);
        if (req.getDiagnosis() != null) sr.setDiagnosis(req.getDiagnosis());
        if (req.getRepairCost() != null) sr.setRepairCost(req.getRepairCost());
        if (req.getTechnicianId() != null) sr.setTechnicianId(req.getTechnicianId());
        if (newStatus == ServiceRequest.ServiceStatus.done) sr.setCompletedAt(LocalDateTime.now());
        if (newStatus == ServiceRequest.ServiceStatus.returned) sr.setReturnedAt(LocalDateTime.now());

        return ResponseEntity.ok(ApiResponse.success(
            toResponse(serviceRequestRepo.save(sr)), "Đã cập nhật trạng thái"));
    }

    @PutMapping("/service-requests/{id}/approve-repair")
    public ResponseEntity<ApiResponse<ServiceRequestResponse>> approveRepair(
            @PathVariable Long id, Authentication auth, @RequestParam boolean approved) {

        Long userId = (Long) auth.getPrincipal();
        ServiceRequest sr = serviceRequestRepo.findById(id)
            .orElseThrow(() -> AppException.notFound("Yêu cầu sửa chữa"));

        if (!sr.getUser().getId().equals(userId)) {
            throw AppException.forbidden("Không có quyền duyệt yêu cầu này");
        }
        if (sr.getRepairCost() == null || sr.getRepairCost().compareTo(BigDecimal.ZERO) <= 0) {
            throw AppException.badRequest("NO_REPAIR_COST", "Chưa có báo giá sửa chữa để duyệt");
        }

        sr.setCustomerApprovedRepair(approved);
        sr.setApprovedAt(LocalDateTime.now());

        return ResponseEntity.ok(ApiResponse.success(
            toResponse(serviceRequestRepo.save(sr)),
            approved ? "Đã duyệt báo giá sửa chữa" : "Đã từ chối báo giá sửa chữa"));
    }

    private ServiceRequestResponse toResponse(ServiceRequest sr) {
        List<String> mediaUrls = serviceMediaRepo.findByServiceRequestIdOrderBySortOrderAsc(sr.getId())
            .stream().map(ServiceMedia::getMediaUrl).toList();
        String technicianName = sr.getTechnicianId() != null
            ? userRepo.findById(sr.getTechnicianId()).map(User::getFullName).orElse(null) : null;
        return buildResponse(sr, mediaUrls, technicianName);
    }

    /** Batch-fetch media/technician names once per list call instead of per row (tránh N+1). */
    private List<ServiceRequestResponse> toResponseList(List<ServiceRequest> requests) {
        if (requests.isEmpty()) return List.of();

        List<Long> ids = requests.stream().map(ServiceRequest::getId).toList();
        Map<Long, List<String>> mediaByRequestId = serviceMediaRepo
            .findByServiceRequestIdInOrderBySortOrderAsc(ids).stream()
            .collect(java.util.stream.Collectors.groupingBy(
                m -> m.getServiceRequest().getId(),
                java.util.stream.Collectors.mapping(ServiceMedia::getMediaUrl, java.util.stream.Collectors.toList())));

        List<Long> technicianIds = requests.stream()
            .map(ServiceRequest::getTechnicianId).filter(java.util.Objects::nonNull).distinct().toList();
        Map<Long, String> technicianNameById = technicianIds.isEmpty() ? Map.of()
            : userRepo.findAllById(technicianIds).stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, User::getFullName));

        return requests.stream()
            .map(sr -> buildResponse(sr,
                mediaByRequestId.getOrDefault(sr.getId(), List.of()),
                technicianNameById.get(sr.getTechnicianId())))
            .toList();
    }

    private ServiceRequestResponse buildResponse(ServiceRequest sr, List<String> mediaUrls, String technicianName) {
        return ServiceRequestResponse.builder()
            .id(sr.getId())
            .serviceCode(sr.getServiceCode())
            .warrantyId(sr.getWarranty() != null ? sr.getWarranty().getId() : null)
            .productName(sr.getProductName())
            .serialNumber(sr.getSerialNumber())
            .issueDesc(sr.getIssueDesc())
            .status(sr.getStatus().name())
            .diagnosis(sr.getDiagnosis())
            .repairCost(sr.getRepairCost())
            .customerApprovedRepair(sr.getCustomerApprovedRepair())
            .approvedAt(sr.getApprovedAt())
            .receivedAt(sr.getReceivedAt())
            .completedAt(sr.getCompletedAt())
            .returnedAt(sr.getReturnedAt())
            .technicianId(sr.getTechnicianId())
            .technicianName(technicianName)
            .createdAt(sr.getCreatedAt())
            .mediaUrls(mediaUrls)
            .userId(sr.getUser() != null ? sr.getUser().getId() : null)
            .userName(sr.getUser() != null ? sr.getUser().getFullName() : null)
            .userPhone(sr.getUser() != null ? sr.getUser().getPhone() : null)
            .storeId(sr.getStoreId())
            .build();
    }

    private String generateServiceCode() {
        String year = String.valueOf(LocalDateTime.now().getYear());
        return "SV-" + year + "-" + String.format("%06d", sequence.getAndIncrement());
    }
}
