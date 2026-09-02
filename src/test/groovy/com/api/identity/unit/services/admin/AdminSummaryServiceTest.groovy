package com.api.identity.unit.services.admin

import com.api.identity.enums.InvitationStatus
import com.api.identity.repositories.UserRepository
import com.api.identity.repositories.WorkspaceInvitationRepository
import com.api.identity.repositories.WorkspaceRepository
import com.api.identity.services.admin.AdminSummaryService
import spock.lang.Specification

class AdminSummaryServiceTest extends Specification {

    UserRepository userRepository = Mock(UserRepository)
    WorkspaceRepository workspaceRepository = Mock(WorkspaceRepository)
    WorkspaceInvitationRepository workspaceInvitationRepository = Mock(WorkspaceInvitationRepository)

    AdminSummaryService service = new AdminSummaryService(
            userRepository, workspaceRepository, workspaceInvitationRepository)

    def "getSummary - aggregates counts from all three repositories"() {
        given:
        userRepository.count() >> 42L
        workspaceRepository.countByIsActiveTrue() >> 17L
        workspaceRepository.countByIsActiveTrueAndCreatedAtGreaterThanEqual(_) >> 3L
        workspaceInvitationRepository.countByStatus(InvitationStatus.PENDING) >> 5L

        when:
        def result = service.getSummary()

        then:
        result.totalUsers() == 42L
        result.totalWorkspaces() == 17L
        result.workspacesCreatedThisMonth() == 3L
        result.pendingInvitations() == 5L
    }
}
