package com.kinhduanpc.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StoreDTO {
    private Long id;
    private String name;
    private String slug;
    private String address;
    private String province;
    private String district;
    private String phone;
    private String email;
    private String openHours;
    private String googleMapsUrl;
    private Boolean isActive;
}
