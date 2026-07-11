package com.api.identity.services;

import com.api.identity.entities.User;
import com.api.identity.enums.UserType;
import com.api.identity.exceptions.EntityNotFoundException;
import com.api.identity.exceptions.PermissionDeniedException;
import com.api.identity.records.UserMe;
import com.api.identity.repositories.OnboardingDoneRepository;
import com.api.identity.repositories.UserRepository;
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
public class UserService {
    private final UserRepository userRepository;
    private final OnboardingDoneRepository onboardingDoneRepository;

    @Transactional(readOnly = true)
    public User getAuthenticatedUser() {
        String email = Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName)
                .orElseThrow(() -> new PermissionDeniedException("Usuario no autenticado"));

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuario inexistente"));
    }
    public UserMe getMe() {
        return null;
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
    public void changeUserType(UserType newUserType) {
        String email = Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName)
                .orElseThrow(() -> new PermissionDeniedException("Usuario no autenticado"));

        int updated = userRepository.updateUserType(email, newUserType);
        if (updated == 0) {
            throw new EntityNotFoundException("Usuario inexistente");
        }
    }
}
