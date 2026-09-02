package com.api.identity.records.admin;

import com.api.identity.enums.WorkspaceRole;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Vista admin-wide de un workspace: la contracara de {@link AdminUserSummaryDTO} — en vez de
 * "por usuario, a qué workspaces pertenece", acá es "por workspace, quién es miembro y con qué
 * rol". Solo workspaces activos (ver {@code Workspace.isActive}).
 */
public record AdminWorkspaceSummaryDTO(
        Long id,
        String name,
        LocalDateTime createdAt,
        List<MemberSummary> members) implements Serializable {

    public record MemberSummary(
            Long userId,
            String email,
            String givenName,
            String familyName,
            WorkspaceRole role,
            LocalDateTime joinedAt) implements Serializable {
    }
}
