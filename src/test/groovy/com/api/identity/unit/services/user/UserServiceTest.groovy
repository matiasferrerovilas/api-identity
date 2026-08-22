package com.api.identity.unit.services.user

import com.api.identity.entities.User
import com.api.identity.exceptions.EntityNotFoundException
import com.api.identity.mappers.UserMapper
import com.api.identity.repositories.OnboardingDoneRepository
import com.api.identity.repositories.UserRepository
import com.api.identity.services.user.UserService
import spock.lang.Specification

/** Covers lookupUserByEmail — used by other services (e.g. api-keep) to resolve an email to a
 * user id before creating a user-to-user file share. */
class UserServiceTest extends Specification {

    UserRepository userRepository = Mock(UserRepository)
    OnboardingDoneRepository onboardingDoneRepository = Mock(OnboardingDoneRepository)
    UserMapper userMapper = Mock(UserMapper)

    UserService service = new UserService(userRepository, onboardingDoneRepository, userMapper)

    def "lookupUserByEmail - returns the id and email when a user with that email exists"() {
        given:
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
        userRepository.findByEmail("nobody@example.com") >> Optional.empty()

        when:
        service.lookupUserByEmail("nobody@example.com")

        then:
        thrown(EntityNotFoundException)
    }
}
