package com.api.identity.unit.services.workspace

import com.api.identity.entities.User
import com.api.identity.entities.Workspace
import com.api.identity.entities.WorkspaceMember
import com.api.identity.enums.AuditAction
import com.api.identity.enums.WorkspaceRole
import com.api.identity.exceptions.EntityAlreadyExistsException
import com.api.identity.exceptions.EntityNotFoundException
import com.api.identity.exceptions.PermissionDeniedException
import com.api.identity.repositories.WorkspaceMemberRepository
import com.api.identity.repositories.WorkspaceRepository
import com.api.identity.services.audit.AuditLogService
import com.api.identity.services.workspace.WorkspaceMembershipService
import spock.lang.Specification
import spock.lang.Unroll

class WorkspaceMembershipServiceTest extends Specification {

    WorkspaceMemberRepository workspaceMemberRepository = Mock(WorkspaceMemberRepository)
    WorkspaceRepository workspaceRepository = Mock(WorkspaceRepository)
    AuditLogService auditLogService = Mock(AuditLogService)

    WorkspaceMembershipService service = new WorkspaceMembershipService(
            workspaceMemberRepository, workspaceRepository, auditLogService)

    def "verifyMembership - does not throw when the user belongs to the workspace"() {
        given:
        workspaceMemberRepository.existsByWorkspaceIdAndUserId(1L, 2L) >> true

        when:
        service.verifyMembership(1L, 2L)

        then:
        noExceptionThrown()
    }

    def "verifyMembership - throws EntityNotFoundException when the user does not belong to the workspace"() {
        given:
        workspaceMemberRepository.existsByWorkspaceIdAndUserId(1L, 2L) >> false

        when:
        service.verifyMembership(1L, 2L)

        then:
        thrown(EntityNotFoundException)
    }

    @Unroll
    def "verifyCanInvite - allows role #role to invite"() {
        given:
        def member = WorkspaceMember.builder().role(role).build()
        workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 2L) >> Optional.of(member)

        when:
        service.verifyCanInvite(1L, 2L)

        then:
        noExceptionThrown()

        where:
        role << [WorkspaceRole.OWNER, WorkspaceRole.COLLABORATOR]
    }

    def "verifyCanInvite - blocks READ_ONLY members from inviting"() {
        given:
        def member = WorkspaceMember.builder().role(WorkspaceRole.READ_ONLY).build()
        workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 2L) >> Optional.of(member)

        when:
        service.verifyCanInvite(1L, 2L)

        then:
        thrown(PermissionDeniedException)
    }

    def "verifyCanInvite - throws EntityNotFoundException when the user is not a member"() {
        given:
        workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 2L) >> Optional.empty()

        when:
        service.verifyCanInvite(1L, 2L)

        then:
        thrown(EntityNotFoundException)
    }

    @Unroll
    def "verifyCanViewAuditLog - allows role #role to view"() {
        given:
        def member = WorkspaceMember.builder().role(role).build()
        workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 2L) >> Optional.of(member)

        when:
        service.verifyCanViewAuditLog(1L, 2L)

        then:
        noExceptionThrown()

        where:
        role << [WorkspaceRole.OWNER, WorkspaceRole.COLLABORATOR]
    }

    def "verifyCanViewAuditLog - blocks READ_ONLY members from viewing"() {
        given:
        def member = WorkspaceMember.builder().role(WorkspaceRole.READ_ONLY).build()
        workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 2L) >> Optional.of(member)

        when:
        service.verifyCanViewAuditLog(1L, 2L)

        then:
        thrown(PermissionDeniedException)
    }

    def "verifyCanViewAuditLog - throws EntityNotFoundException when the user is not a member"() {
        given:
        workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 2L) >> Optional.empty()

        when:
        service.verifyCanViewAuditLog(1L, 2L)

        then:
        thrown(EntityNotFoundException)
    }

    def "addMembership - saves a new COLLABORATOR membership and records a MEMBER_JOINED audit entry"() {
        given:
        def user = User.builder().id(2L).email("a@b.com").build()
        def workspace = Workspace.builder().id(1L).build()
        workspaceMemberRepository.existsByWorkspaceIdAndUserId(1L, 2L) >> false
        workspaceRepository.findById(1L) >> Optional.of(workspace)

        when:
        service.addMembership(1L, user)

        then:
        1 * workspaceMemberRepository.save({ WorkspaceMember m -> m.role == WorkspaceRole.COLLABORATOR && m.user == user })
        1 * auditLogService.record(workspace, AuditAction.MEMBER_JOINED, user, null)
    }

    def "addMembership - throws EntityAlreadyExistsException when already a member"() {
        given:
        def user = User.builder().id(2L).build()
        workspaceMemberRepository.existsByWorkspaceIdAndUserId(1L, 2L) >> true

        when:
        service.addMembership(1L, user)

        then:
        thrown(EntityAlreadyExistsException)
    }

    def "addMembership - throws EntityNotFoundException when the workspace does not exist"() {
        given:
        def user = User.builder().id(2L).build()
        workspaceMemberRepository.existsByWorkspaceIdAndUserId(1L, 2L) >> false
        workspaceRepository.findById(1L) >> Optional.empty()

        when:
        service.addMembership(1L, user)

        then:
        thrown(EntityNotFoundException)
    }
}
