package com.api.identity.services.audit;

import com.api.identity.entities.AuditLog;
import com.api.identity.entities.User;
import com.api.identity.entities.Workspace;
import com.api.identity.enums.AuditAction;
import com.api.identity.mappers.AuditLogMapper;
import com.api.identity.records.audit.AuditLogDTO;
import com.api.identity.repositories.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Auditoría mínima de membresía de workspace: quién invitó, aceptó/rechazó, entró o salió.
 * Sin lógica de permisos acá a propósito — quien llama a {@link #record} ya validó la acción que
 * está registrando, y {@link #getByWorkspace} no chequea permisos para evitar una dependencia
 * circular con {@link com.api.identity.services.workspace.WorkspaceMembershipService} (el chequeo
 * de "quién puede ver la auditoría" vive en el caller, ver {@code WorkspaceService}).
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;

    @Transactional
    public void record(Workspace workspace, AuditAction action, User actor, User targetUser) {
        auditLogRepository.save(AuditLog.builder()
                .workspace(workspace)
                .action(action)
                .actor(actor)
                .targetUser(targetUser)
                .build());
    }

    @Transactional(readOnly = true)
    public List<AuditLogDTO> getByWorkspace(Long workspaceId) {
        return auditLogMapper.toDTO(auditLogRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId));
    }
}
