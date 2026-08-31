package com.api.identity.records.workspaces;

import com.api.identity.enums.WorkspaceRole;
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
        List<@Email(message = "Uno o más emails no son válidos") String> emails,
        // Rol con el que se une cada invitado si acepta. Aplica a toda la tanda — no hay forma de
        // invitar a distintas personas con distintos roles en un mismo llamado. OWNER se rechaza
        // en el service: no se puede invitar directamente como OWNER.
        @NotNull(message = "El rol es requerido")
        WorkspaceRole role) {
}
