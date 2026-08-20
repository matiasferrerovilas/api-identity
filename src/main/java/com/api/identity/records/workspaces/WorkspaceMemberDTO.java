package com.api.identity.records.workspaces;

import com.api.identity.enums.WorkspaceRole;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public record WorkspaceMemberDTO(
        Long id,
        Long workspaceId,
        String workspaceName,
        Metadata metadata) implements Serializable {

    public record Metadata(
            List<String> members,
            List<MemberDetail> memberDetails,
            WorkspaceRole role,
            LocalDateTime joinedAt) implements Serializable {
    }

    /**
     * Detalle por miembro (id de usuario, email, rol) — {@code members} solo trae emails por
     * compatibilidad con consumidores existentes; esto es lo que un cliente necesita para, por
     * ejemplo, ofrecer "eliminar a este miembro" (que requiere el userId, no el email).
     */
    public record MemberDetail(Long userId, String email, WorkspaceRole role) implements Serializable {
    }
}
