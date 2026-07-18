package com.api.identity.records.invitations;

import jakarta.validation.constraints.NotNull;

public record AcceptRejectInvitationDTO(
        @NotNull(message = "El id de la invitación es requerido")
        Long id,
        boolean status) {
}
