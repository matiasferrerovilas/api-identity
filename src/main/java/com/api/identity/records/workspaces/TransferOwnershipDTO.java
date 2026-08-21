package com.api.identity.records.workspaces;

import jakarta.validation.constraints.NotNull;

public record TransferOwnershipDTO(
        @NotNull(message = "El id del nuevo owner es requerido")
        Long newOwnerUserId) {
}
