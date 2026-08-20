package com.api.identity.services.onboarding;

import com.api.identity.entities.User;
import com.api.identity.enums.UserRole;
import com.api.identity.exceptions.EntityNotFoundException;
import com.api.identity.exceptions.PermissionDeniedException;
import com.api.identity.records.onboarding.OnboardingStartRequest;
import com.api.identity.records.onboarding.OnboardingStartResponse;
import com.api.identity.repositories.OnboardingDoneRepository;
import com.api.identity.repositories.UserRepository;
import com.api.identity.services.user.UserAddService;
import com.api.identity.services.user.UserService;
import com.api.identity.services.workspace.WorkspaceAddService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class OnboardingService {
    private final OnboardingDoneRepository onboardingDoneRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final UserAddService userAddService;
    private final WorkspaceAddService workspaceAddService;

    /**
     * Crea el usuario (o lo reutiliza si ya existe) y sus workspaces iniciales en una única
     * transacción — si la creación de workspaces falla, el usuario recién creado también se
     * revierte, en vez de quedar huérfano como pasaba orquestando dos llamadas HTTP separadas
     * desde cada app cliente.
     */
    @Transactional
    public OnboardingStartResponse start(OnboardingStartRequest request, String api) {
        var user = userAddService.createLogInUser(request.user(), api);
        var workspaces = workspaceAddService.createWorkspaces(request.workspaces());
        return new OnboardingStartResponse(user, workspaces);
    }

    @Transactional
    public void markTourAsSeen(String api) {
        String email = Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName)
                .orElseThrow(() -> new PermissionDeniedException("Usuario no autenticado"));

        int updated = onboardingDoneRepository.markTourAsSeen(email, api);
        if (updated == 0) {
            throw new EntityNotFoundException("No existe onboarding para el usuario '%s' y la API '%s'".formatted(email, api));
        }
    }

    @Transactional
    public void changeUserFirstLoginStatus(Long userId) {
        User authenticatedUser = userService.getAuthenticatedUser();
        boolean isAdmin = authenticatedUser.getUserRoles().contains(UserRole.ROLE_ADMIN);

        if (!isAdmin && !authenticatedUser.getId().equals(userId)) {
            throw new PermissionDeniedException("No podés modificar el first-login de otro usuario");
        }

        userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario inexistente"));

        onboardingDoneRepository.markFirstLoginAsDone(userId);
    }
}
