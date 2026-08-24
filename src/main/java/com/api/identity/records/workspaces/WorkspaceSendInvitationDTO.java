package com.api.identity.records.workspaces;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record WorkspaceSendInvitationDTO(
        @NotNull(message = "El workspace es requerido")
        Long workspaceId,
        // El rate limit (10 tandas/hora) asume tandas de tamaño normal — sin este tope, un solo
        // llamado con miles de emails lo evadía por completo.
        @NotEmpty(message = "Se requiere al menos un email")
        @Size(max = 20, message = "No se pueden invitar más de 20 personas por vez")
        List<@Email(message = "Uno o más emails no son válidos") String> emails) {
}
