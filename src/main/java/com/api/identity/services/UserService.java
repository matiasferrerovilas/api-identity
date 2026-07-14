package com.api.identity.services;

import com.api.identity.entities.OnboardingDone;
import com.api.identity.entities.User;
import com.api.identity.enums.UserType;
import com.api.identity.exceptions.EntityNotFoundException;
import com.api.identity.exceptions.PermissionDeniedException;
import com.api.identity.mappers.UserMapper;
import com.api.identity.records.UserMe;
import com.api.identity.repositories.OnboardingDoneRepository;
import com.api.identity.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final OnboardingDoneRepository onboardingDoneRepository;
    private final UserMapper userMapper;

    public UserMe getMe(String api) {
        String email = Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName)
                .orElseThrow(() -> new PermissionDeniedException("Usuario no autenticado"));

        List<String> roles = Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getAuthorities)
                .map(authorities -> authorities.stream()
                        .map(GrantedAuthority::getAuthority)
                        .filter(Objects::nonNull)
                        .filter(authority -> authority.startsWith("ROLE_"))
                        .toList())
                .orElseThrow(() -> new PermissionDeniedException("Usuario no autenticado"));

        var optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isEmpty()) {
            log.warn("Usuario no encontrado {}", email);
            return UserMe.builder()
                    .email(email)
                    .metadata(UserMe.Metadata
                            .builder()
                            .isFirstLogin(true)
                            .hasSeenTour(false)
                            .userRole(roles)
                            .build())
                    .build();
        }

        var onboarding = onboardingDoneRepository.findByUser_EmailAndApi(email, api);

        return userMapper.toUserMe(
                optionalUser.get(),
                onboarding.isEmpty(),
                onboarding.map(OnboardingDone::isHasSeenTour).orElse(false),
                roles);
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
