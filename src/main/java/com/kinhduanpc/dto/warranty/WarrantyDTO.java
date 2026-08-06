package com.kinhduanpc.dto.warranty;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarrantyDTO {
    private Long id;
    private String serialNumber;
    private LocalDate purchaseDate;
    private LocalDate warrantyExpiresAt;
    private Integer warrantyMonths;
    private String status;
    private ProductInfo product;
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductInfo {
        private Long id;
        private String name;
        private String thumbnail;
    }
}
