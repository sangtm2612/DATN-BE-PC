package com.kinhduanpc.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AssignVoucherRequest {
    
    @NotEmpty(message = "Danh sách user không được trống")
    private List<Long> userIds;
    
    private LocalDateTime expiresAt; // Optional: custom expiry date for this assignment
}
