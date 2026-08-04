package com.kinhduanpc.dto.warranty;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ServiceRequestRequest {
    private Long warrantyId; // optional - neu khong chon thi phai nhap productName thu cong
    private String productName;
    private String serialNumber;

    @NotBlank(message = "Vui lòng mô tả lỗi gặp phải")
    private String issueDesc;

    private List<String> mediaUrls; // toi da 5, upload truoc qua /upload/image
}
