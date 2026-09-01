package com.api.identity.records.admin;

import com.api.identity.enums.UserRole;
import com.api.identity.enums.UserType;
import com.api.identity.enums.WorkspaceRole;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Vista admin-wide de un usuario: a diferencia de {@code UserMe}/{@code WorkspaceMemberDTO}
 * (siempre scoped al usuario autenticado o a un workspace puntual), esto es lo que un ROLE_ADMIN
 * ve del listado global — todos los usuarios de la instancia, cada uno con los workspaces
 * activos a los que pertenece.
 */
public record AdminUserSummaryDTO(
        Long id,
        String email,
        String givenName,
        String familyName,
        UserType userType,
        Set<UserRole> userRoles,
        LocalDateTime createdAt,
        List<WorkspaceMembershipSummary> workspaces,
        List<OnboardingSummary> onboarding) implements Serializable {

    public record WorkspaceMembershipSummary(
            Long workspaceId,
            String workspaceName,
            WorkspaceRole role,
            LocalDateTime joinedAt) implements Serializable {
    }

    /** Ojo: el onboarding/tour es por (usuario, api) — api-movements y api-keep se onboardean
     * por separado — no está atado a ningún workspace puntual, a diferencia de
     * {@link WorkspaceMembershipSummary}. */
    public record OnboardingSummary(
            String api,
            boolean isFirstLogin,
            boolean hasSeenTour) implements Serializable {
    }
}
