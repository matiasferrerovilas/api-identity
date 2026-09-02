package com.api.identity.records.admin;

import java.io.Serializable;

/**
 * Números agregados para la portada del panel admin (fe-identity): sin esto, lo único que existe
 * hoy son dos listados planos (usuarios y workspaces) sin ningún total a la vista.
 */
public record AdminSummaryDTO(
        long totalUsers,
        long totalWorkspaces,
        long workspacesCreatedThisMonth,
        long pendingInvitations) implements Serializable {
}
