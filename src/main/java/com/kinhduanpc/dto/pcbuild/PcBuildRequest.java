package com.kinhduanpc.dto.pcbuild;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class PcBuildRequest {

    private String name;
    private String description;

    @NotEmpty(message = "Cấu hình cần có ít nhất 1 linh kiện")
    private List<@Valid Item> items;

    @Data
    public static class Item {
        @NotNull(message = "Thiếu componentTypeId")
        private Long componentTypeId;
        @NotNull(message = "Thiếu productId")
        private Long productId;
        private Integer quantity;
    }
}
