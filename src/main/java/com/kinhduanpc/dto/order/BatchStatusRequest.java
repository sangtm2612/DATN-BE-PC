package com.kinhduanpc.dto.order;

import lombok.Data;
import java.util.List;

@Data
public class BatchStatusRequest {
    private List<Long> ids;
    private String status;
    private String staffNote;
}
