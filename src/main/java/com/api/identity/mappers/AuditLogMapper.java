package com.api.identity.mappers;

import com.api.identity.entities.AuditLog;
import com.api.identity.records.audit.AuditLogDTO;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AuditLogMapper {

    public List<AuditLogDTO> toDTO(List<AuditLog> auditLogs) {
        return auditLogs.stream().map(this::toDTO).toList();
    }

    private AuditLogDTO toDTO(AuditLog auditLog) {
        return new AuditLogDTO(
                auditLog.getId(),
                auditLog.getAction(),
                auditLog.getActor().getEmail(),
                auditLog.getTargetUser() != null ? auditLog.getTargetUser().getEmail() : null,
                auditLog.getCreatedAt());
    }
}
