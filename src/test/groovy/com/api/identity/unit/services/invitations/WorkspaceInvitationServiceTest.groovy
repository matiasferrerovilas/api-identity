package com.api.identity.unit.services.invitations

import com.api.identity.entities.User
import com.api.identity.entities.Workspace
import com.api.identity.entities.WorkspaceInvitation
import com.api.identity.enums.AuditAction
import com.api.identity.enums.InvitationStatus
import com.api.identity.exceptions.BusinessException
import com.api.identity.exceptions.EntityNotFoundException
import com.api.identity.exceptions.PermissionDeniedException
import com.api.identity.exceptions.RateLimitExceededException
import com.api.identity.mappers.WorkspaceInvitationMapper
import com.api.identity.records.invitations.AcceptRejectInvitationDTO
import com.api.identity.records.workspaces.WorkspaceSendInvitationDTO
import com.api.identity.records.workspaces.WorkspaceSentInvitationDTO
import com.api.identity.repositories.WorkspaceInvitationRepository
import com.api.identity.services.audit.AuditLogService
import com.api.identity.services.invitations.InvitationEventPublisher
import com.api.identity.services.invitations.WorkspaceInvitationService
import com.api.identity.services.ratelimit.RateLimiterService
import com.api.identity.services.user.UserService
import com.api.identity.services.workspace.WorkspaceMembershipService
import com.api.identity.services.workspace.WorkspaceService
import org.slf4j.MDC
import spock.lang.Specification

class WorkspaceInvitationServiceTest extends Specification {

    WorkspaceInvitationRepository workspaceInvitationRepository = Mock(WorkspaceInvitationRepository)
    WorkspaceInvitationMapper workspaceInvitationMapper = Mock(WorkspaceInvitationMapper)
    UserService userService = Mock(UserService)
    WorkspaceMembershipService workspaceMembershipService = Mock(WorkspaceMembershipService)
    WorkspaceService workspaceService = Mock(WorkspaceService)
    InvitationEventPublisher invitationEventPublisher = Mock(InvitationEventPublisher)
    RateLimiterService rateLimiterService = Mock(RateLimiterService)
    AuditLogService auditLogService = Mock(AuditLogService)

    WorkspaceInvitationService service = new WorkspaceInvitationService(
            workspaceInvitationRepository,
            workspaceInvitationMapper,
            userService,
            workspaceMembershipService,
            workspaceService,
            invitationEventPublisher,
            rateLimiterService,
            auditLogService)

    // Nada de default global acá a propósito: en Spock, la interacción declarada PRIMERO gana
    // cuando varias matchean (no la última) — un default de setup()/field siempre le gana a un
    // stub más específico puesto después en el given: de un test puntual. Cada test que necesita
    // que el rate limiter deje pasar lo stubea explícitamente; el test que verifica el rechazo
    // no stubea nada y aprovecha que un Mock sin stub devuelve `false` para un boolean.
    def inviter = User.builder().id(1L).email("owner@example.com").build()
    def invited = User.builder().id(2L).email("invited@example.com").build()
    def workspace = Workspace.builder().id(10L).name("Casa").build()

    def cleanup() {
        MDC.clear()
    }

    def "sendInvitation - requires the caller to be allowed to invite before creating anything"() {
        given:
        userService.getAuthenticatedUser() >> inviter
        rateLimiterService.tryAcquire(_, _, _) >> true
        workspaceService.findWorkspaceById(10L) >> workspace
        userService.getUserByEmail(["invited@example.com"]) >> [invited]
        workspaceInvitationRepository.findByWorkspaceIdAndStatusAndInvitedUserId(10L, InvitationStatus.PENDING, 2L) >> Optional.empty()
        workspaceInvitationRepository.save(_ as WorkspaceInvitation) >> { WorkspaceInvitation wi -> wi }

        when:
        service.sendInvitation(10L, new WorkspaceSendInvitationDTO(10L, ["invited@example.com"]))

        then:
        1 * workspaceMembershipService.verifyCanInvite(10L, 1L)
        1 * invitationEventPublisher.publishInvitationCreated({ it.invitedUserEmail() == "invited@example.com" })
        1 * auditLogService.record(workspace, AuditAction.INVITATION_SENT, inviter, invited)
    }

    def "sendInvitation - stamps the published event with the request's correlation id"() {
        given:
        MDC.put("correlationId", "trace-abc")
        userService.getAuthenticatedUser() >> inviter
        rateLimiterService.tryAcquire(_, _, _) >> true
        workspaceService.findWorkspaceById(10L) >> workspace
        userService.getUserByEmail(["invited@example.com"]) >> [invited]
        workspaceInvitationRepository.findByWorkspaceIdAndStatusAndInvitedUserId(10L, InvitationStatus.PENDING, 2L) >> Optional.empty()
        workspaceInvitationRepository.save(_ as WorkspaceInvitation) >> { WorkspaceInvitation wi -> wi }

        when:
        service.sendInvitation(10L, new WorkspaceSendInvitationDTO(10L, ["invited@example.com"]))

        then:
        1 * invitationEventPublisher.publishInvitationCreated({ it.correlationId() == "trace-abc" })
    }

    def "sendInvitation - a READ_ONLY member is rejected before any invitation is created"() {
        given:
        userService.getAuthenticatedUser() >> inviter
        rateLimiterService.tryAcquire(_, _, _) >> true
        workspaceMembershipService.verifyCanInvite(10L, 1L) >> { throw new PermissionDeniedException("Los miembros de solo lectura no pueden invitar a otros usuarios") }

        when:
        service.sendInvitation(10L, new WorkspaceSendInvitationDTO(10L, ["invited@example.com"]))

        then:
        thrown(PermissionDeniedException)
        0 * workspaceInvitationRepository.save(_)
    }

    def "sendInvitation - throws RateLimitExceededException and creates nothing when the limiter rejects"() {
        given:
        userService.getAuthenticatedUser() >> inviter
        // Sin stub para tryAcquire: un Mock sin interacción declarada devuelve `false` para un
        // método que retorna boolean, que es justo el escenario "límite excedido" que queremos.

        when:
        service.sendInvitation(10L, new WorkspaceSendInvitationDTO(10L, ["invited@example.com"]))

        then:
        thrown(RateLimitExceededException)
        0 * workspaceMembershipService.verifyCanInvite(_, _)
        0 * workspaceInvitationRepository.save(_)
    }

    def "sendInvitation - skips creating a duplicate when a pending invitation already exists"() {
        given:
        userService.getAuthenticatedUser() >> inviter
        rateLimiterService.tryAcquire(_, _, _) >> true
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
        1 * auditLogService.record(workspace, AuditAction.INVITATION_ACCEPTED, invited, null)
        1 * invitationEventPublisher.publishInvitationAccepted({ it.invitationId() == 5L && it.acceptedByEmail() == "invited@example.com" })
    }

    def "acceptRejectInvitation - stamps the published event with the request's correlation id"() {
        given:
        def invitation = WorkspaceInvitation.builder()
                .id(5L).invitedUser(invited).invitedBy(inviter).workspace(workspace)
                .status(InvitationStatus.PENDING).build()
        MDC.put("correlationId", "trace-xyz")
        userService.getAuthenticatedUser() >> invited
        workspaceInvitationRepository.findById(5L) >> Optional.of(invitation)

        when:
        service.acceptRejectInvitation(new AcceptRejectInvitationDTO(5L, true))

        then:
        1 * invitationEventPublisher.publishInvitationAccepted({ it.correlationId() == "trace-xyz" })
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
        1 * auditLogService.record(workspace, AuditAction.INVITATION_REJECTED, invited, null)
        0 * invitationEventPublisher.publishInvitationAccepted(_)
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

    def "getSentInvitations - returns the authenticated user's sent invitations, most recent first"() {
        given:
        def sent = [WorkspaceInvitation.builder().id(7L).invitedUser(invited).invitedBy(inviter).workspace(workspace)
                .status(InvitationStatus.PENDING).build()]
        def dto = new WorkspaceSentInvitationDTO(7L, 10L, "Casa", "invited@example.com", InvitationStatus.PENDING, null)
        userService.getAuthenticatedUser() >> inviter
        workspaceInvitationRepository.findByInvitedByIdOrderByCreatedAtDesc(1L) >> sent
        workspaceInvitationMapper.toSentDTO(sent) >> [dto]

        when:
        def result = service.getSentInvitations()

        then:
        result == [dto]
    }

    def "cancelInvitation - marks a pending invitation sent by the caller as CANCELLED"() {
        given:
        def invitation = WorkspaceInvitation.builder()
                .id(5L).invitedUser(invited).invitedBy(inviter).workspace(workspace)
                .status(InvitationStatus.PENDING).build()
        userService.getAuthenticatedUser() >> inviter
        workspaceInvitationRepository.findById(5L) >> Optional.of(invitation)

        when:
        service.cancelInvitation(5L)

        then:
        invitation.status == InvitationStatus.CANCELLED
        1 * workspaceInvitationRepository.save(invitation)
        1 * auditLogService.record(workspace, AuditAction.INVITATION_CANCELLED, inviter, invited)
    }

    def "cancelInvitation - throws EntityNotFoundException when the caller did not send the invitation"() {
        given:
        def stranger = User.builder().id(99L).email("stranger@example.com").build()
        def invitation = WorkspaceInvitation.builder()
                .id(5L).invitedUser(invited).invitedBy(inviter).workspace(workspace)
                .status(InvitationStatus.PENDING).build()
        userService.getAuthenticatedUser() >> stranger
        workspaceInvitationRepository.findById(5L) >> Optional.of(invitation)

        when:
        service.cancelInvitation(5L)

        then:
        thrown(EntityNotFoundException)
        0 * workspaceInvitationRepository.save(_)
    }

    def "cancelInvitation - throws BusinessException when the invitation was already answered"() {
        given:
        def invitation = WorkspaceInvitation.builder()
                .id(5L).invitedUser(invited).invitedBy(inviter).workspace(workspace)
                .status(InvitationStatus.ACCEPTED).build()
        userService.getAuthenticatedUser() >> inviter
        workspaceInvitationRepository.findById(5L) >> Optional.of(invitation)

        when:
        service.cancelInvitation(5L)

        then:
        thrown(BusinessException)
        0 * workspaceInvitationRepository.save(_)
    }
}
