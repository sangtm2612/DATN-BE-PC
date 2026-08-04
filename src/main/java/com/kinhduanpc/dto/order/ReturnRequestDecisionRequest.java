package com.kinhduanpc.dto.order;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReturnRequestDecisionRequest {
    @NotBlank(message = "Vui lòng chọn quyết định")
    private String decision; // approved, rejected

    private String resolution; // exchange, refund - bat buoc neu decision=approved
    private BigDecimal refundAmount; // bat buoc neu resolution=refund
    private String staffNote;
}
