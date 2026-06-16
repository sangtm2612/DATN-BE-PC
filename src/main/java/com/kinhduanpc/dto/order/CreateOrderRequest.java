package com.kinhduanpc.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateOrderRequest {

    // Địa chỉ giao hàng
    private Long addressId; // dùng địa chỉ có sẵn hoặc nhập mới

    @NotBlank
    private String shippingName;
    @NotBlank
    private String shippingPhone;
    @NotBlank
    private String shippingProvince;
    @NotBlank
    private String shippingDistrict;
    @NotBlank
    private String shippingWard;
    @NotBlank
    private String shippingAddress;

    @NotNull
    private String paymentMethod; // cod, bank_transfer, vnpay, momo, zalopay, installment

    private Long shippingMethodId;
    private Long pickupStoreId; // nếu nhận tại cửa hàng

    private String voucherCode;
    private String note;

    // Trả góp
    private Long installmentPlanId;
}
