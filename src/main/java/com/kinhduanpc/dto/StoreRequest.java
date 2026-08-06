package com.kinhduanpc.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StoreRequest {
    @NotBlank(message = "Tên cửa hàng không được để trống")
    private String name;
    
    @NotBlank(message = "Địa chỉ không được để trống")
    private String address;
    
    @NotBlank(message = "Tỉnh/thành không được để trống")
    private String province;
    
    private String district;
    
    @NotBlank(message = "Số điện thoại không được để trống")
    private String phone;
    
    @Email(message = "Email không hợp lệ")
    private String email;
    
    private String openHours;
    private String googleMapsUrl;
}
