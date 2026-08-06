package com.kinhduanpc.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserAddressRequest {
    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;
    
    @NotBlank(message = "Số điện thoại không được để trống")
    private String phone;
    
    @NotBlank(message = "Tỉnh/thành không được để trống")
    private String province;
    
    @NotBlank(message = "Quận/huyện không được để trống")
    private String district;
    
    @NotBlank(message = "Phường/xã không được để trống")
    private String ward;
    
    @NotBlank(message = "Địa chỉ chi tiết không được để trống")
    private String addressDetail;
    
    private Boolean isDefault;
}
