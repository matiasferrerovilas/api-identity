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
            List<MemberDetail> memberDetails,
            WorkspaceRole role,
            LocalDateTime joinedAt) implements Serializable {
    }

    /**
     * Detalle por miembro (id de usuario, email, rol) — antes había también un {@code members:
     * string[]} con solo los emails, redundante con esto (un cliente puede derivarlo con
     * {@code memberDetails.map(m => m.email)}) y sin lo que hace falta para, por ejemplo, ofrecer
     * "eliminar a este miembro" (que requiere el userId, no el email). Se sacó el campo duplicado.
     */
    public record MemberDetail(Long userId, String email, WorkspaceRole role) implements Serializable {
    }
}
