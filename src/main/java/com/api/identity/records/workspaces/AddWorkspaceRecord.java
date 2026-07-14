package com.api.identity.records.workspaces;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record AddWorkspaceRecord(
        Long userId,
        @NotBlank(message = "El nombre del workspace es requerido")
        List<String> workspaces) {
}
