package com.api.identity.security;

import com.api.identity.exceptions.PermissionDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Punto único para leer identidad/roles del contexto de seguridad. Antes de esto, la misma
 * cadena SecurityContextHolder...filter(isAuthenticated)...orElseThrow estaba copiada 7 veces
 * entre UserService, OnboardingService y UserAddService, cada una con su propio mensaje/orden.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Authentication currentAuthentication() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .orElseThrow(() -> new PermissionDeniedException("Usuario no autenticado"));
    }

    public static String currentEmail() {
        return currentAuthentication().getName();
    }

    public static List<String> currentRoles() {
        return currentAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .filter(authority -> authority.startsWith("ROLE_"))
                .toList();
    }
}
