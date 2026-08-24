package com.api.identity.services.user;

import com.api.identity.entities.OnboardingDone;
import com.api.identity.entities.User;
import com.api.identity.enums.UserRole;
import com.api.identity.exceptions.EntityAlreadyExistsException;
import com.api.identity.exceptions.PermissionDeniedException;
import com.api.identity.exceptions.RateLimitExceededException;
import com.api.identity.mappers.UserMapper;
import com.api.identity.records.user.UserMe;
import com.api.identity.records.user.UserToAdd;
import com.api.identity.repositories.OnboardingDoneRepository;
import com.api.identity.repositories.UserRepository;
import com.api.identity.services.api.ApiService;
import com.api.identity.services.ratelimit.RateLimiterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
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

    // Generoso a propósito: un usuario real onboardea una vez por API, pero puede reintentar
    // tras un error de red o abrir la app en otro dispositivo — esto solo corta un loop/abuso.
    private static final int MAX_USER_CREATIONS_PER_HOUR = 20;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofHours(1);

    private final UserRepository userRepository;
    private final OnboardingDoneRepository onboardingDoneRepository;
    private final UserMapper userMapper;
    private final ApiService apiService;
    private final RateLimiterService rateLimiterService;

    @Transactional
    public UserMe createLogInUser(UserToAdd request, String api) {
        Authentication authentication = Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .orElseThrow(() -> new PermissionDeniedException("Usuario no autenticado"));

        // El email del JWT es la única fuente confiable de identidad acá — sin este chequeo,
        // cualquier llamador autenticado podía mandar el email de otra persona en el body y
        // colgar su onboarding (y su rate limit) de una cuenta ajena.
        String authenticatedEmail = authentication.getName();
        if (authenticatedEmail == null || !authenticatedEmail.equalsIgnoreCase(request.email())) {
            throw new PermissionDeniedException("El email no coincide con el de la cuenta autenticada");
        }

        var rateLimitKey = "rate-limit:user-creation:" + authenticatedEmail;
        if (!rateLimiterService.tryAcquire(rateLimitKey, MAX_USER_CREATIONS_PER_HOUR, RATE_LIMIT_WINDOW)) {
            throw new RateLimitExceededException("Demasiados intentos. Probá de nuevo más tarde.");
        }

        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .filter(authority -> authority.startsWith("ROLE_"))
                .toList();

        Set<UserRole> userRoles = roles.stream()
                .map(UserRole::parse)
                .filter(VALID_ROLES::contains)
                .collect(Collectors.toCollection(HashSet::new));

        var existingUser = userRepository.findByEmail(request.email());

        if (existingUser.isPresent() && onboardingDoneRepository.findByUserEmailAndApiName(request.email(), api).isPresent()) {
            throw new EntityAlreadyExistsException(
                    "El usuario con email '%s' ya completó el onboarding para '%s'".formatted(request.email(), api));
        }

        var user = existingUser.orElseGet(() -> userRepository.save(User.builder()
                .email(request.email())
                .givenName(request.givenName())
                .familyName(request.familyName())
                .userType(request.userType())
                .userRoles(userRoles)
                .build()));

        var onboarding = onboardingDoneRepository.save(OnboardingDone.builder()
                .user(user)
                .api(apiService.getOrCreate(api))
                .isFirstLogin(false)
                .hasSeenTour(false)
                .build());

        return userMapper.toUserMe(user, false, onboarding.isHasSeenTour(), roles);
    }
}
