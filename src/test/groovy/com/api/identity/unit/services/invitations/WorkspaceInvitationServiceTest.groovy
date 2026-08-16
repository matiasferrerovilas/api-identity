package com.api.identity.unit.services.invitations

import com.api.identity.entities.User
import com.api.identity.entities.Workspace
import com.api.identity.entities.WorkspaceInvitation
import com.api.identity.enums.InvitationStatus
import com.api.identity.exceptions.BusinessException
import com.api.identity.exceptions.EntityNotFoundException
import com.api.identity.exceptions.PermissionDeniedException
import com.api.identity.mappers.WorkspaceInvitationMapper
import com.api.identity.records.invitations.AcceptRejectInvitationDTO
import com.api.identity.records.workspaces.WorkspaceSendInvitationDTO
import com.api.identity.repositories.WorkspaceInvitationRepository
import com.api.identity.services.invitations.InvitationEventPublisher
import com.api.identity.services.invitations.WorkspaceInvitationService
import com.api.identity.services.user.UserService
import com.api.identity.services.workspace.WorkspaceMembershipService
import com.api.identity.services.workspace.WorkspaceService
import spock.lang.Specification

class WorkspaceInvitationServiceTest extends Specification {

    WorkspaceInvitationRepository workspaceInvitationRepository = Mock(WorkspaceInvitationRepository)
    WorkspaceInvitationMapper workspaceInvitationMapper = Mock(WorkspaceInvitationMapper)
    UserService userService = Mock(UserService)
    WorkspaceMembershipService workspaceMembershipService = Mock(WorkspaceMembershipService)
    WorkspaceService workspaceService = Mock(WorkspaceService)
    InvitationEventPublisher invitationEventPublisher = Mock(InvitationEventPublisher)

    WorkspaceInvitationService service = new WorkspaceInvitationService(
            workspaceInvitationRepository,
            workspaceInvitationMapper,
            userService,
            workspaceMembershipService,
            workspaceService,
            invitationEventPublisher)

    def inviter = User.builder().id(1L).email("owner@example.com").build()
    def invited = User.builder().id(2L).email("invited@example.com").build()
    def workspace = Workspace.builder().id(10L).name("Casa").build()

    def "sendInvitation - requires the caller to be allowed to invite before creating anything"() {
        given:
        userService.getAuthenticatedUser() >> inviter
        workspaceService.findWorkspaceById(10L) >> workspace
        userService.getUserByEmail(["invited@example.com"]) >> [invited]
        workspaceInvitationRepository.findByWorkspaceIdAndStatusAndInvitedUserId(10L, InvitationStatus.PENDING, 2L) >> Optional.empty()
        workspaceInvitationRepository.save(_ as WorkspaceInvitation) >> { WorkspaceInvitation wi -> wi }

        when:
        service.sendInvitation(10L, new WorkspaceSendInvitationDTO(10L, ["invited@example.com"]))

        then:
        1 * workspaceMembershipService.verifyCanInvite(10L, 1L)
        1 * invitationEventPublisher.publishInvitationCreated({ it.invitedUserEmail() == "invited@example.com" })
    }

    def "sendInvitation - a READ_ONLY member is rejected before any invitation is created"() {
        given:
        userService.getAuthenticatedUser() >> inviter
        workspaceMembershipService.verifyCanInvite(10L, 1L) >> { throw new PermissionDeniedException("Los miembros de solo lectura no pueden invitar a otros usuarios") }

        when:
        service.sendInvitation(10L, new WorkspaceSendInvitationDTO(10L, ["invited@example.com"]))

        then:
        thrown(PermissionDeniedException)
        0 * workspaceInvitationRepository.save(_)
    }

    def "sendInvitation - skips creating a duplicate when a pending invitation already exists"() {
        given:
        userService.getAuthenticatedUser() >> inviter
        workspaceService.findWorkspaceById(10L) >> workspace
        userService.getUserByEmail(["invited@example.com"]) >> [invited]
        workspaceInvitationRepository.findByWorkspaceIdAndStatusAndInvitedUserId(10L, InvitationStatus.PENDING, 2L) >>
                Optional.of(WorkspaceInvitation.builder().id(99L).build())

        when:
        service.sendInvitation(10L, new WorkspaceSendInvitationDTO(10L, ["invited@example.com"]))

        then:
        0 * workspaceInvitationRepository.save(_)
    }

    def "acceptRejectInvitation - accepting adds membership and marks the invitation ACCEPTED"() {
        given:
        def invitation = WorkspaceInvitation.builder()
                .id(5L).invitedUser(invited).invitedBy(inviter).workspace(workspace)
                .status(InvitationStatus.PENDING).build()
        userService.getAuthenticatedUser() >> invited
        workspaceInvitationRepository.findById(5L) >> Optional.of(invitation)

        when:
        service.acceptRejectInvitation(new AcceptRejectInvitationDTO(5L, true))

        then:
        invitation.status == InvitationStatus.ACCEPTED
        1 * workspaceMembershipService.addMembership(10L, invited)
        1 * workspaceInvitationRepository.save(invitation)
    }

    def "acceptRejectInvitation - rejecting does not add membership"() {
        given:
        def invitation = WorkspaceInvitation.builder()
                .id(5L).invitedUser(invited).invitedBy(inviter).workspace(workspace)
                .status(InvitationStatus.PENDING).build()
        userService.getAuthenticatedUser() >> invited
        workspaceInvitationRepository.findById(5L) >> Optional.of(invitation)

        when:
        service.acceptRejectInvitation(new AcceptRejectInvitationDTO(5L, false))

        then:
        invitation.status == InvitationStatus.REJECTED
        0 * workspaceMembershipService.addMembership(_, _)
    }

    def "acceptRejectInvitation - throws when responding to someone else's invitation"() {
        given:
        def stranger = User.builder().id(99L).email("stranger@example.com").build()
        def invitation = WorkspaceInvitation.builder()
                .id(5L).invitedUser(invited).invitedBy(inviter).workspace(workspace)
                .status(InvitationStatus.PENDING).build()
        userService.getAuthenticatedUser() >> stranger
        workspaceInvitationRepository.findById(5L) >> Optional.of(invitation)

        when:
        service.acceptRejectInvitation(new AcceptRejectInvitationDTO(5L, true))

        then:
        thrown(EntityNotFoundException)
    }

    def "acceptRejectInvitation - throws BusinessException when already answered"() {
        given:
        def invitation = WorkspaceInvitation.builder()
                .id(5L).invitedUser(invited).invitedBy(inviter).workspace(workspace)
                .status(InvitationStatus.ACCEPTED).build()
        userService.getAuthenticatedUser() >> invited
        workspaceInvitationRepository.findById(5L) >> Optional.of(invitation)

        when:
        service.acceptRejectInvitation(new AcceptRejectInvitationDTO(5L, true))

        then:
        thrown(BusinessException)
    }
}
