package com.api.identity.records.workspaces;

import com.api.identity.enums.InvitationStatus;
import com.api.identity.enums.WorkspaceRole;

import java.io.Serializable;
import java.time.LocalDateTime;

public record WorkspaceSentInvitationDTO(
        Long id,
        Long workspaceId,
        String workspaceName,
        String invitedUserEmail,
        InvitationStatus status,
        WorkspaceRole role,
        LocalDateTime createdAt) implements Serializable {
}
