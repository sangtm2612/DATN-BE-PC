package com.kinhduanpc.dto.warranty;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceRequestResponse {
    private Long id;
    private String serviceCode;
    private Long warrantyId;
    private String productName;
    private String serialNumber;
    private String issueDesc;
    private String status;
    private String diagnosis;
    private BigDecimal repairCost;
    private Boolean customerApprovedRepair;
    private LocalDateTime approvedAt;
    private LocalDateTime receivedAt;
    private LocalDateTime completedAt;
    private LocalDateTime returnedAt;
    private Long technicianId;
    private String technicianName;
    private LocalDateTime createdAt;
    private List<String> mediaUrls;

    // Danh cho admin/staff xem
    private Long userId;
    private String userName;
    private String userPhone;
    private Long storeId;
}
