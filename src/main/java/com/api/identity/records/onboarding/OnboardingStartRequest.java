package com.api.identity.records.onboarding;

import com.api.identity.records.user.UserToAdd;
import com.api.identity.records.workspaces.AddWorkspaceRecord;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Crea el usuario y sus workspaces iniciales en una sola llamada atómica — ver {@link
 * com.api.identity.services.onboarding.OnboardingService#start}. Reemplaza el par
 * {@code POST /v1/users} + {@code POST /v1/workspaces} que cada app orquestaba por separado, sin
 * ninguna atomicidad entre ambas llamadas.
 */
public record OnboardingStartRequest(
        @NotNull @Valid UserToAdd user,
        @NotEmpty List<@Valid AddWorkspaceRecord> workspaces) {
}
