package com.api.identity.records.workspaces;

import com.api.identity.enums.WorkspaceRole;
import jakarta.validation.constraints.NotNull;

public record ChangeMemberRoleDTO(
        @NotNull(message = "El nuevo rol es requerido")
        WorkspaceRole newRole) {
}
