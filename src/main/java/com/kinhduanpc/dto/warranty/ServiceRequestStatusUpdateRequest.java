package com.kinhduanpc.dto.warranty;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ServiceRequestStatusUpdateRequest {
    @NotBlank(message = "Vui lòng chọn trạng thái")
    private String status; // received, diagnosing, repairing, waiting_part, done, returned

    private String diagnosis;
    private BigDecimal repairCost;
    private Long technicianId;
}
