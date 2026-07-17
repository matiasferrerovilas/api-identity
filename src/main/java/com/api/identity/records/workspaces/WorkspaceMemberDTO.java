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
            WorkspaceRole role,
            LocalDateTime joinedAt) implements Serializable {
    }
}
