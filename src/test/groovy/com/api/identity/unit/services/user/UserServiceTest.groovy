package com.api.identity.unit.services.user

import com.api.identity.entities.User
import com.api.identity.entities.Workspace
import com.api.identity.entities.WorkspaceMember
import com.api.identity.enums.UserRole
import com.api.identity.enums.UserType
import com.api.identity.enums.WorkspaceRole
import com.api.identity.exceptions.BusinessException
import com.api.identity.exceptions.EntityNotFoundException
import com.api.identity.exceptions.PermissionDeniedException
import com.api.identity.exceptions.RateLimitExceededException
import com.api.identity.mappers.AdminUserMapper
import com.api.identity.mappers.UserMapper
import com.api.identity.records.admin.AdminUserSummaryDTO
import com.api.identity.records.user.UserMe
import com.api.identity.repositories.OnboardingDoneRepository
import com.api.identity.repositories.UserRepository
import com.api.identity.repositories.WorkspaceMemberRepository
import com.api.identity.services.ratelimit.RateLimiterService
import com.api.identity.services.user.UserService
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import spock.lang.Specification

/** Covers lookupUserByEmail/getUsersByIds — used by other services (e.g. api-keep) to resolve an
 * email/ids to user data, and rate-limited/size-capped against account enumeration since neither
 * requires the caller to share a workspace with the target. */
class UserServiceTest extends Specification {

    UserRepository userRepository = Mock(UserRepository)
    OnboardingDoneRepository onboardingDoneRepository = Mock(OnboardingDoneRepository)
    WorkspaceMemberRepository workspaceMemberRepository = Mock(WorkspaceMemberRepository)
    UserMapper userMapper = Mock(UserMapper)
    AdminUserMapper adminUserMapper = Mock(AdminUserMapper)
    RateLimiterService rateLimiterService = Mock(RateLimiterService)

    UserService service = new UserService(
            userRepository, onboardingDoneRepository, workspaceMemberRepository, userMapper, adminUserMapper, rateLimiterService)

    def setup() {
        authenticateAs("caller@example.com")
    }

    def cleanup() {
        SecurityContextHolder.clearContext()
    }

    def "lookupUserByEmail - returns the id and email when a user with that email exists"() {
        given:
        rateLimiterService.tryAcquire(_, _, _) >> true
        User user = User.builder().id(7L).email("alice@example.com").build()
        userRepository.findByEmail("alice@example.com") >> Optional.of(user)

        when:
        def result = service.lookupUserByEmail("alice@example.com")

        then:
        result.id() == 7L
        result.email() == "alice@example.com"
    }

    def "lookupUserByEmail - throws EntityNotFoundException when no user matches that email"() {
        given:
        rateLimiterService.tryAcquire(_, _, _) >> true
        userRepository.findByEmail("nobody@example.com") >> Optional.empty()

        when:
        service.lookupUserByEmail("nobody@example.com")

        then:
        thrown(EntityNotFoundException)
    }

    def "lookupUserByEmail - throws RateLimitExceededException when the caller exceeds the lookup limit"() {
        given:
        rateLimiterService.tryAcquire("rate-limit:user-lookup:caller@example.com", 30, _) >> false

        when:
        service.lookupUserByEmail("alice@example.com")

        then:
        thrown(RateLimitExceededException)
        0 * userRepository.findByEmail(_ as String)
    }

    def "lookupUserByEmail - throws PermissionDeniedException when there is no authenticated caller"() {
        given:
        SecurityContextHolder.clearContext()

        when:
        service.lookupUserByEmail("alice@example.com")

        then:
        thrown(PermissionDeniedException)
    }

    def "getUsersByIds - returns the mapped users when within the rate limit and size cap"() {
        given:
        rateLimiterService.tryAcquire(_, _, _) >> true
        def users = [User.builder().id(1L).email("a@example.com").build()]
        userRepository.findAllById([1L]) >> users
        userMapper.toUserMe(users) >> [UserMe.builder().email("a@example.com").build()]

        when:
        def result = service.getUsersByIds([1L])

        then:
        result.size() == 1
        result[0].email() == "a@example.com"
    }

    def "getUsersByIds - throws BusinessException when more than 100 ids are requested"() {
        given:
        def ids = (1L..101L).toList()

        when:
        service.getUsersByIds(ids)

        then:
        thrown(BusinessException)
        0 * userRepository.findAllById(_)
    }

    def "getUsersByIds - throws RateLimitExceededException when the caller exceeds the lookup limit"() {
        given:
        rateLimiterService.tryAcquire("rate-limit:user-lookup:caller@example.com", 30, _) >> false

        when:
        service.getUsersByIds([1L])

        then:
        thrown(RateLimitExceededException)
        0 * userRepository.findAllById(_)
    }

    def "getMe - includes the caller's role in the given workspace"() {
        given:
        def user = User.builder().id(1L).email("caller@example.com").build()
        def workspace = Workspace.builder().id(5L).name("Casa").build()
        def membership = WorkspaceMember.builder().id(50L).workspace(workspace).user(user).role(WorkspaceRole.COLLABORATOR).build()
        userRepository.findByEmail("caller@example.com") >> Optional.of(user)
        onboardingDoneRepository.findByUserEmailAndApiName("caller@example.com", "api-movements") >> Optional.empty()
        userMapper.toUserMe(user, true, false, ["ROLE_FAMILY"]) >> UserMe.builder().id(1L).email("caller@example.com")
                .metadata(UserMe.Metadata.builder().isFirstLogin(true).hasSeenTour(false).userRole(["ROLE_FAMILY"]).build())
                .build()
        workspaceMemberRepository.findByWorkspaceIdAndUserId(5L, 1L) >> Optional.of(membership)

        when:
        def result = service.getMe("api-movements", 5L)

        then:
        result.metadata().workspaceRole() == WorkspaceRole.COLLABORATOR
    }

    def "getMe - workspaceRole is null when the caller is not a member of the given workspace"() {
        given:
        def user = User.builder().id(1L).email("caller@example.com").build()
        userRepository.findByEmail("caller@example.com") >> Optional.of(user)
        onboardingDoneRepository.findByUserEmailAndApiName("caller@example.com", "api-movements") >> Optional.empty()
        userMapper.toUserMe(user, true, false, ["ROLE_FAMILY"]) >> UserMe.builder().id(1L).email("caller@example.com")
                .metadata(UserMe.Metadata.builder().isFirstLogin(true).hasSeenTour(false).userRole(["ROLE_FAMILY"]).build())
                .build()
        workspaceMemberRepository.findByWorkspaceIdAndUserId(99L, 1L) >> Optional.empty()

        when:
        def result = service.getMe("api-movements", 99L)

        then:
        result.metadata().workspaceRole() == null
    }

    def "getMe - does not look up any membership when no workspaceId is given"() {
        given:
        def user = User.builder().id(1L).email("caller@example.com").build()
        userRepository.findByEmail("caller@example.com") >> Optional.of(user)
        onboardingDoneRepository.findByUserEmailAndApiName("caller@example.com", "api-movements") >> Optional.empty()
        userMapper.toUserMe(user, true, false, ["ROLE_FAMILY"]) >> UserMe.builder().id(1L).email("caller@example.com")
                .metadata(UserMe.Metadata.builder().isFirstLogin(true).hasSeenTour(false).userRole(["ROLE_FAMILY"]).build())
                .build()

        when:
        def result = service.getMe("api-movements")

        then:
        result.metadata().workspaceRole() == null
        0 * workspaceMemberRepository.findByWorkspaceIdAndUserId(_, _)
    }

    def "listAllUsersWithWorkspaces - groups active memberships by user id and delegates the mapping to AdminUserMapper"() {
        given:
        def alice = User.builder().id(1L).email("alice@example.com").build()
        def bob = User.builder().id(2L).email("bob@example.com").build()
        userRepository.findAll() >> [alice, bob]

        def casa = Workspace.builder().id(10L).name("Casa").build()
        def trabajo = Workspace.builder().id(11L).name("Trabajo").build()
        def aliceInCasa = WorkspaceMember.builder().id(100L).workspace(casa).user(alice).role(WorkspaceRole.OWNER).build()
        def aliceInTrabajo = WorkspaceMember.builder().id(101L).workspace(trabajo).user(alice).role(WorkspaceRole.COLLABORATOR).build()
        def bobInCasa = WorkspaceMember.builder().id(102L).workspace(casa).user(bob).role(WorkspaceRole.READ_ONLY).build()
        workspaceMemberRepository.findAllActiveWithWorkspaceAndUser() >> [aliceInCasa, aliceInTrabajo, bobInCasa]

        // Contenido arbitrario — records, no mocks, así el == de más abajo compara por valor sin
        // depender de que Spock sepa mockear clases final.
        def aliceWorkspaces = [new AdminUserSummaryDTO.WorkspaceMembershipSummary(10L, "Casa", WorkspaceRole.OWNER, null)]
        def bobWorkspaces = [new AdminUserSummaryDTO.WorkspaceMembershipSummary(10L, "Casa", WorkspaceRole.READ_ONLY, null)]
        def aliceDTO = new AdminUserSummaryDTO(1L, "alice@example.com", null, null, UserType.PERSONAL, [] as Set, null, aliceWorkspaces)
        def bobDTO = new AdminUserSummaryDTO(2L, "bob@example.com", null, null, UserType.PERSONAL, [] as Set, null, bobWorkspaces)

        when:
        def result = service.listAllUsersWithWorkspaces()

        then:
        1 * adminUserMapper.toWorkspaceMembershipSummaries([aliceInCasa, aliceInTrabajo]) >> aliceWorkspaces
        1 * adminUserMapper.toWorkspaceMembershipSummaries([bobInCasa]) >> bobWorkspaces
        1 * adminUserMapper.toAdminUserSummaryDTO(alice, aliceWorkspaces) >> aliceDTO
        1 * adminUserMapper.toAdminUserSummaryDTO(bob, bobWorkspaces) >> bobDTO
        result == [aliceDTO, bobDTO]
    }

    def "listAllUsersWithWorkspaces - a user with no memberships gets an empty list passed to the mapper"() {
        given:
        def carol = User.builder().id(3L).email("carol@example.com").build()
        userRepository.findAll() >> [carol]
        workspaceMemberRepository.findAllActiveWithWorkspaceAndUser() >> []
        def carolDTO = new AdminUserSummaryDTO(3L, "carol@example.com", null, null, UserType.PERSONAL, [] as Set, null, [])

        when:
        def result = service.listAllUsersWithWorkspaces()

        then:
        1 * adminUserMapper.toWorkspaceMembershipSummaries([]) >> []
        1 * adminUserMapper.toAdminUserSummaryDTO(carol, []) >> carolDTO
        result == [carolDTO]
    }

    private static void authenticateAs(String email) {
        def auth = new UsernamePasswordAuthenticationToken(email, null, [new SimpleGrantedAuthority("ROLE_FAMILY")])
        SecurityContextHolder.getContext().setAuthentication(auth)
    }
}
