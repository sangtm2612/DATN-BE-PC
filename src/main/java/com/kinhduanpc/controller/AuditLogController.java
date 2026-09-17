package com.kinhduanpc.controller;

import com.kinhduanpc.dto.ApiResponse;
import com.kinhduanpc.service.AuditLogService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','STAFF','TECHNICIAN')")
@Tag(name = "Audit Logs", description = "Lịch sử thay đổi các đối tượng")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping("/{entityType}/{entityId}")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getLogs(
            @PathVariable String entityType,
            @PathVariable Long entityId) {
        return ResponseEntity.ok(ApiResponse.success(
            auditLogService.getLogs(entityType.toUpperCase(), entityId)));
    }
}
