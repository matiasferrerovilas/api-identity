package com.api.identity.unit.services.onboarding

import com.api.identity.enums.UserType
import com.api.identity.records.onboarding.OnboardingStartRequest
import com.api.identity.records.user.UserMe
import com.api.identity.records.user.UserToAdd
import com.api.identity.records.workspaces.AddWorkspaceRecord
import com.api.identity.records.workspaces.WorkspaceAdded
import com.api.identity.repositories.OnboardingDoneRepository
import com.api.identity.repositories.UserRepository
import com.api.identity.services.onboarding.OnboardingService
import com.api.identity.services.user.UserAddService
import com.api.identity.services.user.UserService
import com.api.identity.services.workspace.WorkspaceAddService
import spock.lang.Specification

class OnboardingServiceTest extends Specification {

    OnboardingDoneRepository onboardingDoneRepository = Mock()
    UserRepository userRepository = Mock()
    UserService userService = Mock()
    UserAddService userAddService = Mock()
    WorkspaceAddService workspaceAddService = Mock()

    OnboardingService service

    def setup() {
        service = new OnboardingService(
                onboardingDoneRepository, userRepository, userService, userAddService, workspaceAddService)
    }

    def "start - creates the user and their workspaces in a single call, returning both"() {
        given:
        def userToAdd = new UserToAdd("new@example.com", "New", "User", true, UserType.PERSONAL)
        def workspaces = [new AddWorkspaceRecord("DEFAULT")]
        def request = new OnboardingStartRequest(userToAdd, workspaces)
        def createdUser = new UserMe(1L, "new@example.com", "New", "User", "PERSONAL", null)
        def createdWorkspaces = [new WorkspaceAdded(10L, "DEFAULT")]

        userAddService.createLogInUser(userToAdd, "api-movements") >> createdUser
        workspaceAddService.createWorkspaces(workspaces) >> createdWorkspaces

        when:
        def result = service.start(request, "api-movements")

        then:
        result.user() == createdUser
        result.workspaces() == createdWorkspaces
    }

    def "start - propagates a failure creating workspaces without swallowing it"() {
        given:
        def userToAdd = new UserToAdd("new@example.com", "New", "User", true, UserType.PERSONAL)
        def request = new OnboardingStartRequest(userToAdd, [new AddWorkspaceRecord("")])
        def createdUser = new UserMe(1L, "new@example.com", "New", "User", "PERSONAL", null)

        userAddService.createLogInUser(userToAdd, "api-keep") >> createdUser
        workspaceAddService.createWorkspaces(_ as List) >> { throw new RuntimeException("boom") }

        when:
        service.start(request, "api-keep")

        then:
        thrown(RuntimeException)
    }
}
