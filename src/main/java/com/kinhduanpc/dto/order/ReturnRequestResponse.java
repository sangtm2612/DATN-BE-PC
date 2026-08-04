package com.kinhduanpc.dto.order;

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
public class ReturnRequestResponse {
    private Long id;
    private String returnCode;
    private Long orderId;
    private String orderCode;
    private String status;
    private String reasonType;
    private String reasonDetail;
    private String resolution;
    private BigDecimal refundAmount;
    private String staffNote;
    private String reviewedByName;
    private LocalDateTime reviewedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private List<String> mediaUrls;
    private List<ItemResponse> items;

    // Danh cho admin/staff xem
    private Long userId;
    private String userName;
    private String userPhone;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ItemResponse {
        private Long orderItemId;
        private String productName;
        private Integer quantity;
    }
}
