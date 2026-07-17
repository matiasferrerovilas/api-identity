package com.api.identity.records.workspaces;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AddWorkspaceRecord(
        @NotBlank(message = "El nombre del workspace es requerido")
        String description) {
}
