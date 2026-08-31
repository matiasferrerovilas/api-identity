package com.api.identity.services.user;

import com.api.identity.entities.OnboardingDone;
import com.api.identity.entities.User;
import com.api.identity.entities.WorkspaceMember;
import com.api.identity.enums.UserType;
import com.api.identity.exceptions.BusinessException;
import com.api.identity.exceptions.EntityNotFoundException;
import com.api.identity.exceptions.RateLimitExceededException;
import com.api.identity.mappers.UserMapper;
import com.api.identity.records.user.UserLookupDTO;
import com.api.identity.records.user.UserMe;
import com.api.identity.repositories.OnboardingDoneRepository;
import com.api.identity.repositories.UserRepository;
import com.api.identity.repositories.WorkspaceMemberRepository;
import com.api.identity.security.SecurityUtils;
import com.api.identity.services.ratelimit.RateLimiterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    // Ambos endpoints resuelven identidad de cualquier usuario/id sin requerir que el caller
    // comparta workspace con el objetivo — sin este límite, una cuenta autenticada podía probar
    // emails/ids arbitrarios sin fricción y enumerar quién tiene cuenta en toda la suite.
    private static final int MAX_LOOKUPS_PER_HOUR = 30;
    private static final Duration LOOKUP_RATE_LIMIT_WINDOW = Duration.ofHours(1);
    private static final int MAX_IDS_PER_REQUEST = 100;

    private final UserRepository userRepository;
    private final OnboardingDoneRepository onboardingDoneRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserMapper userMapper;
    private final RateLimiterService rateLimiterService;

    @Transactional(readOnly = true)
    public User getAuthenticatedUser() {
        String email = SecurityUtils.currentEmail();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuario inexistente"));
    }

    public UserMe getMe(String api) {
        return getMe(api, null);
    }

    /**
     * @param workspaceId when given, resolves and includes the caller's role in that specific
     *                    workspace ({@link UserMe#workspaceRole()}) — null if the caller isn't a
     *                    member of it. Optional because {@code /v1/users/me} is meaningful with no
     *                    workspace context too (e.g. right after login, before any workspace is
     *                    "active"); api-identity has no notion of "the" active workspace itself
     *                    (that's app-specific state each caller owns), so the caller decides which
     *                    workspace's role it wants, if any.
     */
    public UserMe getMe(String api, Long workspaceId) {
        String email = SecurityUtils.currentEmail();
        List<String> roles = SecurityUtils.currentRoles();

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

        var user = optionalUser.get();
        var onboarding = onboardingDoneRepository.findByUserEmailAndApiName(email, api);

        UserMe userMe = userMapper.toUserMe(
                user,
                onboarding.map(OnboardingDone::isFirstLogin).orElse(true),
                onboarding.map(OnboardingDone::isHasSeenTour).orElse(false),
                roles);

        if (workspaceId == null) {
            return userMe;
        }

        var workspaceRole = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, user.getId())
                .map(WorkspaceMember::getRole)
                .orElse(null);

        return userMe.toBuilder()
                .metadata(userMe.metadata().toBuilder().workspaceRole(workspaceRole).build())
                .build();
    }

    @Transactional(readOnly = true)
    public List<UserMe> getUsersByIds(List<Long> ids) {
        if (ids.size() > MAX_IDS_PER_REQUEST) {
            throw new BusinessException("No se pueden pedir más de " + MAX_IDS_PER_REQUEST + " ids por llamada");
        }
        this.enforceLookupRateLimit();
        return userMapper.toUserMe(userRepository.findAllById(ids));
    }

    @Transactional
    public void changeUserType(UserType newUserType) {
        String email = SecurityUtils.currentEmail();

        int updated = userRepository.updateUserType(email, newUserType);
        if (updated == 0) {
            throw new EntityNotFoundException("Usuario inexistente");
        }
    }

    public List<User> getUserByEmail(List<String> emails) {
        return userRepository.findByEmail(emails);
    }

    /** Used by other services (e.g. api-keep, before creating a user-to-user file share) to
     * resolve an email to a user id — unlike {@link #getUserByEmail}, which silently drops
     * unmatched emails for the invitation flow, this fails loudly so the caller can surface an
     * honest "no account with that email" error instead of a share that silently goes nowhere. */
    public UserLookupDTO lookupUserByEmail(String email) {
        this.enforceLookupRateLimit();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("No existe un usuario con email " + email));
        return new UserLookupDTO(user.getId(), user.getEmail());
    }

    private void enforceLookupRateLimit() {
        String callerEmail = SecurityUtils.currentEmail();

        boolean acquired = rateLimiterService.tryAcquire(
                "rate-limit:user-lookup:" + callerEmail, MAX_LOOKUPS_PER_HOUR, LOOKUP_RATE_LIMIT_WINDOW);
        if (!acquired) {
            throw new RateLimitExceededException("Demasiadas consultas. Probá de nuevo más tarde.");
        }
    }
}
