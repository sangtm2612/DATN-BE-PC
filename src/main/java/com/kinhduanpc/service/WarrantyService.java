package com.kinhduanpc.service;

import com.kinhduanpc.dto.warranty.*;
import com.kinhduanpc.entity.*;
import com.kinhduanpc.exception.AppException;
import com.kinhduanpc.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class WarrantyService {

    private final WarrantyRepository warrantyRepo;
    private final ServiceRequestRepository serviceRequestRepo;
    private final ServiceMediaRepository serviceMediaRepo;
    private final UserRepository userRepo;

    private static final AtomicInteger sequence = new AtomicInteger(1);

    private static final Set<ServiceRequest.ServiceStatus> POST_DIAGNOSIS_STATUSES = Set.of(
            ServiceRequest.ServiceStatus.repairing,
            ServiceRequest.ServiceStatus.waiting_part,
            ServiceRequest.ServiceStatus.done,
            ServiceRequest.ServiceStatus.returned
    );

    @Transactional(readOnly = true)
    public WarrantyDTO lookupBySerial(String serial) {
        Warranty warranty = warrantyRepo.findBySerialNumber(serial)
                .orElseThrow(() -> AppException.notFound("Thông tin bảo hành"));
        return toWarrantyDTO(warranty);
    }

    @Transactional(readOnly = true)
    public WarrantyDTO lookupByOrderCode(String orderCode) {
        List<Warranty> warranties = warrantyRepo.findByOrderCode(orderCode);
        if (warranties.isEmpty()) {
            throw AppException.notFound("Thông tin bảo hành");
        }
        return toWarrantyDTO(warranties.get(0));
    }

    @Transactional(readOnly = true)
    public List<WarrantyDTO> getMyWarranties(Long userId) {
        return warrantyRepo.findByUserId(userId).stream()
                .map(this::toWarrantyDTO)
                .toList();
    }

    public ServiceRequestResponse createServiceRequest(ServiceRequestRequest request, Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> AppException.notFound("Người dùng"));

        Warranty warranty = null;
        String productName = request.getProductName();
        String serialNumber = request.getSerialNumber();

        if (request.getWarrantyId() != null) {
            warranty = warrantyRepo.findById(request.getWarrantyId())
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

        if (request.getMediaUrls() != null && request.getMediaUrls().size() > 5) {
            throw AppException.badRequest("TOO_MANY_MEDIA", "Tối đa 5 file ảnh/video");
        }

        ServiceRequest sr = ServiceRequest.builder()
                .warranty(warranty)
                .user(user)
                .serviceCode(generateServiceCode())
                .productName(productName)
                .serialNumber(serialNumber)
                .issueDesc(request.getIssueDesc())
                .status(ServiceRequest.ServiceStatus.received)
                .build();
        sr = serviceRequestRepo.save(sr);

        if (request.getMediaUrls() != null && !request.getMediaUrls().isEmpty()) {
            List<ServiceMedia> mediaList = new ArrayList<>();
            int i = 0;
            for (String url : request.getMediaUrls()) {
                mediaList.add(ServiceMedia.builder()
                        .serviceRequest(sr)
                        .mediaUrl(url)
                        .mediaType("image")
                        .uploadedBy("customer")
                        .sortOrder(i++)
                        .build());
            }
            serviceMediaRepo.saveAll(mediaList);
        }

        return toServiceRequestResponse(sr);
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestResponse> getMyServiceRequests(Long userId) {
        List<ServiceRequest> requests = serviceRequestRepo.findByUserId(userId).stream()
                .sorted(Comparator.comparing(ServiceRequest::getCreatedAt).reversed())
                .toList();
        return toServiceRequestResponseList(requests);
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestResponse> getAdminServiceRequests(String status, Long storeId) {
        List<ServiceRequest> requests;
        
        if (status != null) {
            try {
                ServiceRequest.ServiceStatus serviceStatus = ServiceRequest.ServiceStatus.valueOf(status);
                requests = serviceRequestRepo.findByStatusOrderByCreatedAtDesc(serviceStatus);
            } catch (IllegalArgumentException e) {
                throw AppException.badRequest("INVALID_STATUS", "Trạng thái không hợp lệ");
            }
        } else {
            requests = serviceRequestRepo.findAllByOrderByCreatedAtDesc();
        }

        if (storeId != null) {
            requests = requests.stream()
                    .filter(r -> storeId.equals(r.getStoreId()))
                    .toList();
        }

        return toServiceRequestResponseList(requests);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTechnicians() {
        return userRepo.findByRole(User.UserRole.technician).stream()
                .map(u -> Map.<String, Object>of("id", u.getId(), "fullName", u.getFullName()))
                .toList();
    }

    public ServiceRequestResponse updateServiceRequestStatus(
            Long id, ServiceRequestStatusUpdateRequest request) {
        ServiceRequest sr = serviceRequestRepo.findById(id)
                .orElseThrow(() -> AppException.notFound("Yêu cầu sửa chữa"));

        ServiceRequest.ServiceStatus newStatus;
        try {
            newStatus = ServiceRequest.ServiceStatus.valueOf(request.getStatus());
        } catch (IllegalArgumentException e) {
            throw AppException.badRequest("INVALID_STATUS", "Trạng thái không hợp lệ");
        }

        BigDecimal effectiveRepairCost = request.getRepairCost() != null 
                ? request.getRepairCost() 
                : sr.getRepairCost();
                
        if (POST_DIAGNOSIS_STATUSES.contains(newStatus)
                && effectiveRepairCost != null 
                && effectiveRepairCost.compareTo(BigDecimal.ZERO) > 0
                && !Boolean.TRUE.equals(sr.getCustomerApprovedRepair())) {
            throw AppException.badRequest("REPAIR_NOT_APPROVED",
                    "Khách hàng chưa duyệt báo giá sửa chữa, chưa thể chuyển sang trạng thái này");
        }

        sr.setStatus(newStatus);
        if (request.getDiagnosis() != null) sr.setDiagnosis(request.getDiagnosis());
        if (request.getRepairCost() != null) sr.setRepairCost(request.getRepairCost());
        if (request.getTechnicianId() != null) sr.setTechnicianId(request.getTechnicianId());
        if (newStatus == ServiceRequest.ServiceStatus.done) sr.setCompletedAt(LocalDateTime.now());
        if (newStatus == ServiceRequest.ServiceStatus.returned) sr.setReturnedAt(LocalDateTime.now());

        return toServiceRequestResponse(serviceRequestRepo.save(sr));
    }

    public ServiceRequestResponse approveRepair(Long id, Long userId, boolean approved) {
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

        return toServiceRequestResponse(serviceRequestRepo.save(sr));
    }

    private String generateServiceCode() {
        String year = String.valueOf(LocalDateTime.now().getYear());
        return "SV-" + year + "-" + String.format("%06d", sequence.getAndIncrement());
    }

    private WarrantyDTO toWarrantyDTO(Warranty warranty) {
        return WarrantyDTO.builder()
                .id(warranty.getId())
                .serialNumber(warranty.getSerialNumber())
                .purchaseDate(warranty.getPurchaseDate())
                .warrantyExpiresAt(warranty.getWarrantyExpiresAt())
                .warrantyMonths(warranty.getWarrantyMonths())
                .status(warranty.getStatus())
                .product(WarrantyDTO.ProductInfo.builder()
                        .id(warranty.getProduct().getId())
                        .name(warranty.getProduct().getName())
                        .thumbnail(warranty.getProduct().getThumbnail())
                        .build())
                .build();
    }

    private ServiceRequestResponse toServiceRequestResponse(ServiceRequest sr) {
        List<String> mediaUrls = serviceMediaRepo.findByServiceRequestIdOrderBySortOrderAsc(sr.getId())
                .stream()
                .map(ServiceMedia::getMediaUrl)
                .toList();
        
        String technicianName = sr.getTechnicianId() != null
                ? userRepo.findById(sr.getTechnicianId()).map(User::getFullName).orElse(null)
                : null;
                
        return buildResponse(sr, mediaUrls, technicianName);
    }

    private List<ServiceRequestResponse> toServiceRequestResponseList(List<ServiceRequest> requests) {
        if (requests.isEmpty()) return List.of();

        // Batch fetch media and technician names to avoid N+1 queries
        List<Long> ids = requests.stream().map(ServiceRequest::getId).toList();
        Map<Long, List<String>> mediaByRequestId = serviceMediaRepo
                .findByServiceRequestIdInOrderBySortOrderAsc(ids).stream()
                .collect(Collectors.groupingBy(
                        m -> m.getServiceRequest().getId(),
                        Collectors.mapping(ServiceMedia::getMediaUrl, Collectors.toList())
                ));

        List<Long> technicianIds = requests.stream()
                .map(ServiceRequest::getTechnicianId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
                
        Map<Long, String> technicianNameById = technicianIds.isEmpty() ? Map.of()
                : userRepo.findAllById(technicianIds).stream()
                        .collect(Collectors.toMap(User::getId, User::getFullName));

        return requests.stream()
                .map(sr -> buildResponse(
                        sr,
                        mediaByRequestId.getOrDefault(sr.getId(), List.of()),
                        sr.getTechnicianId() != null ? technicianNameById.get(sr.getTechnicianId()) : null
                ))
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
}
