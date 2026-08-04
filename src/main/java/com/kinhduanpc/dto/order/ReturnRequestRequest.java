package com.kinhduanpc.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

@Data
public class ReturnRequestRequest {

    @NotEmpty(message = "Vui lòng chọn ít nhất 1 sản phẩm để đổi/trả")
    @Valid
    private List<Item> items;

    @NotBlank(message = "Vui lòng chọn lý do đổi/trả")
    private String reasonType; // defective, wrong_item, damaged_delivery, not_satisfied

    private String reasonDetail;

    private List<String> mediaUrls; // toi da 5, upload truoc qua /upload/image

    @Data
    public static class Item {
        @NotNull
        private Long orderItemId;
        @NotNull @Positive
        private Integer quantity;
    }
}
