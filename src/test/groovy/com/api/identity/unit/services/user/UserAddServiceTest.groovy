package com.api.identity.unit.services.user

import com.api.identity.entities.Api
import com.api.identity.entities.OnboardingDone
import com.api.identity.entities.User
import com.api.identity.enums.UserType
import com.api.identity.exceptions.EntityAlreadyExistsException
import com.api.identity.exceptions.PermissionDeniedException
import com.api.identity.mappers.UserMapper
import com.api.identity.records.user.UserMe
import com.api.identity.records.user.UserToAdd
import com.api.identity.repositories.OnboardingDoneRepository
import com.api.identity.repositories.UserRepository
import com.api.identity.services.api.ApiService
import com.api.identity.services.user.UserAddService
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import spock.lang.Specification

class UserAddServiceTest extends Specification {

    UserRepository userRepository = Mock(UserRepository)
    OnboardingDoneRepository onboardingDoneRepository = Mock(OnboardingDoneRepository)
    UserMapper userMapper = Mock(UserMapper)
    ApiService apiService = Mock(ApiService)

    UserAddService service = new UserAddService(userRepository, onboardingDoneRepository, userMapper, apiService)

    def request = UserToAdd.builder()
            .email("new@example.com").givenName("Nueva").familyName("Persona")
            .isFirstLogin(true).userType(UserType.PERSONAL).build()

    def setup() {
        authenticateAs("new@example.com", "ROLE_FAMILY")
    }

    def cleanup() {
        SecurityContextHolder.clearContext()
    }

    def "createLogInUser - creates a new user and its onboarding row when neither exists"() {
        given:
        userRepository.findByEmail("new@example.com") >> Optional.empty()
        onboardingDoneRepository.findByUserEmailAndApiName("new@example.com", "api-keep") >> Optional.empty()
        apiService.getOrCreate("api-keep") >> Api.builder().id(1L).name("api-keep").build()
        userMapper.toUserMe(_ as User, false, false, ["ROLE_FAMILY"]) >> UserMe.builder().email("new@example.com").build()

        when:
        service.createLogInUser(request, "api-keep")

        then:
        1 * userRepository.save(_ as User) >> { User u -> u }
        1 * onboardingDoneRepository.save(_ as OnboardingDone) >>
                OnboardingDone.builder().id(1L).isFirstLogin(false).hasSeenTour(false).build()
    }

    def "createLogInUser - reuses an existing user and only creates the onboarding row"() {
        given:
        def existing = User.builder().id(7L).email("new@example.com").userType(UserType.PERSONAL).build()
        userRepository.findByEmail("new@example.com") >> Optional.of(existing)
        onboardingDoneRepository.findByUserEmailAndApiName("new@example.com", "api-keep") >> Optional.empty()
        apiService.getOrCreate("api-keep") >> Api.builder().id(1L).name("api-keep").build()
        userMapper.toUserMe(existing, false, false, ["ROLE_FAMILY"]) >> UserMe.builder().email("new@example.com").build()

        when:
        service.createLogInUser(request, "api-keep")

        then:
        0 * userRepository.save(_)
        1 * onboardingDoneRepository.save(_ as OnboardingDone) >>
                OnboardingDone.builder().id(1L).isFirstLogin(false).hasSeenTour(false).build()
    }

    def "createLogInUser - throws when the user already completed onboarding for that api"() {
        given:
        def existing = User.builder().id(7L).email("new@example.com").build()
        userRepository.findByEmail("new@example.com") >> Optional.of(existing)
        onboardingDoneRepository.findByUserEmailAndApiName("new@example.com", "api-keep") >>
                Optional.of(OnboardingDone.builder().id(3L).build())

        when:
        service.createLogInUser(request, "api-keep")

        then:
        thrown(EntityAlreadyExistsException)
    }

    def "createLogInUser - throws PermissionDeniedException when there is no authenticated user"() {
        given:
        SecurityContextHolder.clearContext()

        when:
        service.createLogInUser(request, "api-keep")

        then:
        thrown(PermissionDeniedException)
    }

    private static void authenticateAs(String email, String... roles) {
        def authorities = roles.collect { new SimpleGrantedAuthority(it) }
        def auth = new UsernamePasswordAuthenticationToken(email, null, authorities)
        SecurityContextHolder.getContext().setAuthentication(auth)
    }
}
