package com.api.identity.records.workspaces;

import java.util.List;

public record WorkspaceSendInvitationDTO(Long workspaceId, List<String> emails) {
}
