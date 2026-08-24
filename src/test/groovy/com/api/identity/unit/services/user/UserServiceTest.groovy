package com.api.identity.unit.services.user

import com.api.identity.entities.User
import com.api.identity.exceptions.BusinessException
import com.api.identity.exceptions.EntityNotFoundException
import com.api.identity.exceptions.PermissionDeniedException
import com.api.identity.exceptions.RateLimitExceededException
import com.api.identity.mappers.UserMapper
import com.api.identity.records.user.UserMe
import com.api.identity.repositories.OnboardingDoneRepository
import com.api.identity.repositories.UserRepository
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
    UserMapper userMapper = Mock(UserMapper)
    RateLimiterService rateLimiterService = Mock(RateLimiterService)

    UserService service = new UserService(userRepository, onboardingDoneRepository, userMapper, rateLimiterService)

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

    private static void authenticateAs(String email) {
        def auth = new UsernamePasswordAuthenticationToken(email, null, [new SimpleGrantedAuthority("ROLE_FAMILY")])
        SecurityContextHolder.getContext().setAuthentication(auth)
    }
}
