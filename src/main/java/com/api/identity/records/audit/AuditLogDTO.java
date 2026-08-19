package com.api.identity.records.audit;

import com.api.identity.enums.AuditAction;

import java.io.Serializable;
import java.time.LocalDateTime;

public record AuditLogDTO(
        Long id,
        AuditAction action,
        String actorEmail,
        String targetEmail,
        LocalDateTime createdAt) implements Serializable {
}
