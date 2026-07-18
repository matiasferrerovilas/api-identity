package com.api.identity.records.workspaces;

import jakarta.validation.constraints.NotBlank;

public record AddWorkspaceRecord(
        @NotBlank(message = "El nombre del workspace es requerido")
        String description) {
}
