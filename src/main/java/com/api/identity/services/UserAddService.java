package com.api.identity.services;

import com.api.identity.entities.OnboardingDone;
import com.api.identity.entities.User;
import com.api.identity.enums.UserRole;
import com.api.identity.exceptions.EntityAlreadyExistsException;
import com.api.identity.exceptions.PermissionDeniedException;
import com.api.identity.mappers.UserMapper;
import com.api.identity.records.UserMe;
import com.api.identity.records.UserToAdd;
import com.api.identity.repositories.OnboardingDoneRepository;
import com.api.identity.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserAddService {

    private static final EnumSet<UserRole> VALID_ROLES = EnumSet.allOf(UserRole.class);

    private final UserRepository userRepository;
    private final OnboardingDoneRepository onboardingDoneRepository;
    private final UserMapper userMapper;

    @Transactional
    public UserMe createLogInUser(UserToAdd request, String api) {
        List<String> roles = Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getAuthorities)
                .map(authorities -> authorities.stream()
                        .map(GrantedAuthority::getAuthority)
                        .filter(Objects::nonNull)
                        .filter(authority -> authority.startsWith("ROLE_"))
                        .toList())
                .orElseThrow(() -> new PermissionDeniedException("Usuario no autenticado"));

        Set<UserRole> userRoles = roles.stream()
                .map(UserRole::parse)
                .filter(VALID_ROLES::contains)
                .collect(Collectors.toCollection(HashSet::new));

        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new EntityAlreadyExistsException("Ya existe un usuario con el email '%s'".formatted(request.email()));
        }

        var user = userRepository.save(User.builder()
                .email(request.email())
                .givenName(request.givenName())
                .familyName(request.familyName())
                .userType(request.userType())
                .userRoles(userRoles)
                .build());

        var onboarding = onboardingDoneRepository.save(OnboardingDone.builder()
                .user(user)
                .api(api)
                .isFirstLogin(false)
                .hasSeenTour(false)
                .build());

        return userMapper.toUserMe(user, false, onboarding.isHasSeenTour(), roles);
    }
}
