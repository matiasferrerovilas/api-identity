package com.api.identity.records.workspaces;

import com.api.identity.enums.InvitationStatus;

import java.io.Serializable;
import java.time.LocalDateTime;

public record WorkspaceSentInvitationDTO(
        Long id,
        Long workspaceId,
        String workspaceName,
        String invitedUserEmail,
        InvitationStatus status,
        LocalDateTime createdAt) implements Serializable {
}
