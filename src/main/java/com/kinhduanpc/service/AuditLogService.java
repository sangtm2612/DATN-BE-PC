package com.kinhduanpc.service;

import com.kinhduanpc.entity.AuditLog;
import com.kinhduanpc.entity.User;
import com.kinhduanpc.repository.AuditLogRepository;
import com.kinhduanpc.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepo;
    private final UserRepository userRepo;

    public void log(String entityType, Long entityId, String action,
                    String fromValue, String toValue, String note, Long performedByUserId) {
        User performer = null;
        String performerName = "Hệ thống";
        String performerRole = "system";

        if (performedByUserId != null) {
            performer = userRepo.findById(performedByUserId).orElse(null);
            if (performer != null) {
                performerName = performer.getFullName();
                performerRole = performer.getRole().name();
            }
        }

        AuditLog log = AuditLog.builder()
            .entityType(entityType)
            .entityId(entityId)
            .action(action)
            .fromValue(fromValue)
            .toValue(toValue)
            .note(note)
            .performedBy(performer)
            .performedByName(performerName)
            .performedByRole(performerRole)
            .build();

        auditLogRepo.save(log);
    }

    public List<Map<String, Object>> getLogs(String entityType, Long entityId) {
        return auditLogRepo
            .findByEntityTypeAndEntityIdOrderByCreatedAtAsc(entityType, entityId)
            .stream()
            .map(l -> {
                java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("id",              l.getId());
                m.put("action",          l.getAction());
                m.put("fromValue",       l.getFromValue());
                m.put("toValue",         l.getToValue());
                m.put("note",            l.getNote());
                m.put("performedByName", l.getPerformedByName());
                m.put("performedByRole", l.getPerformedByRole());
                m.put("createdAt",       l.getCreatedAt());
                return m;
            })
            .toList();
    }
}
