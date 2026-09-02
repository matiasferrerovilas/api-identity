package com.api.identity.unit.services.workspace

import com.api.identity.entities.User
import com.api.identity.entities.Workspace
import com.api.identity.entities.WorkspaceMember
import com.api.identity.enums.AuditAction
import com.api.identity.enums.UserRole
import com.api.identity.enums.WorkspaceRole
import com.api.identity.exceptions.EntityAlreadyExistsException
import com.api.identity.exceptions.EntityNotFoundException
import com.api.identity.exceptions.PermissionDeniedException
import com.api.identity.repositories.WorkspaceMemberRepository
import com.api.identity.repositories.WorkspaceRepository
import com.api.identity.events.MemberRemovedEvent
import com.api.identity.services.audit.AuditLogService
import com.api.identity.services.workspace.WorkspaceMembershipEventPublisher
import com.api.identity.services.workspace.WorkspaceMembershipService
import org.slf4j.MDC
import spock.lang.Specification
import spock.lang.Unroll

class WorkspaceMembershipServiceTest extends Specification {

    WorkspaceMemberRepository workspaceMemberRepository = Mock(WorkspaceMemberRepository)
    WorkspaceRepository workspaceRepository = Mock(WorkspaceRepository)
    AuditLogService auditLogService = Mock(AuditLogService)
    WorkspaceMembershipEventPublisher workspaceMembershipEventPublisher = Mock(WorkspaceMembershipEventPublisher)

    WorkspaceMembershipService service = new WorkspaceMembershipService(
            workspaceMemberRepository, workspaceRepository, auditLogService, workspaceMembershipEventPublisher)

    def cleanup() {
        MDC.clear()
    }

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
        def actor = User.builder().id(2L).build()
        def member = WorkspaceMember.builder().role(role).build()
        workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 2L) >> Optional.of(member)

        when:
        service.verifyCanInvite(1L, actor)

        then:
        noExceptionThrown()

        where:
        role << [WorkspaceRole.OWNER, WorkspaceRole.COLLABORATOR]
    }

    def "verifyCanInvite - blocks READ_ONLY members from inviting"() {
        given:
        def actor = User.builder().id(2L).build()
        def member = WorkspaceMember.builder().role(WorkspaceRole.READ_ONLY).build()
        workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 2L) >> Optional.of(member)

        when:
        service.verifyCanInvite(1L, actor)

        then:
        thrown(PermissionDeniedException)
    }

    def "verifyCanInvite - throws EntityNotFoundException when the user is not a member"() {
        given:
        def actor = User.builder().id(2L).build()
        workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 2L) >> Optional.empty()

        when:
        service.verifyCanInvite(1L, actor)

        then:
        thrown(EntityNotFoundException)
    }

    def "verifyCanInvite - a global admin who is not a member can invite anyway"() {
        given:
        def actor = User.builder().id(2L).userRoles([UserRole.ROLE_ADMIN] as Set).build()

        when:
        service.verifyCanInvite(1L, actor)

        then:
        noExceptionThrown()
        0 * workspaceMemberRepository.findByWorkspaceIdAndUserId(_, _)
    }

    @Unroll
    def "verifyCanViewAuditLog - allows role #role to view"() {
        given:
        def actor = User.builder().id(2L).build()
        def member = WorkspaceMember.builder().role(role).build()
        workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 2L) >> Optional.of(member)

        when:
        service.verifyCanViewAuditLog(1L, actor)

        then:
        noExceptionThrown()

        where:
        role << [WorkspaceRole.OWNER, WorkspaceRole.COLLABORATOR]
    }

    def "verifyCanViewAuditLog - blocks READ_ONLY members from viewing"() {
        given:
        def actor = User.builder().id(2L).build()
        def member = WorkspaceMember.builder().role(WorkspaceRole.READ_ONLY).build()
        workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 2L) >> Optional.of(member)

        when:
        service.verifyCanViewAuditLog(1L, actor)

        then:
        thrown(PermissionDeniedException)
    }

    def "verifyCanViewAuditLog - throws EntityNotFoundException when the user is not a member"() {
        given:
        def actor = User.builder().id(2L).build()
        workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 2L) >> Optional.empty()

        when:
        service.verifyCanViewAuditLog(1L, actor)

        then:
        thrown(EntityNotFoundException)
    }

    def "verifyCanViewAuditLog - a global admin who is not a member can view anyway"() {
        given:
        def actor = User.builder().id(2L).userRoles([UserRole.ROLE_ADMIN] as Set).build()

        when:
        service.verifyCanViewAuditLog(1L, actor)

        then:
        noExceptionThrown()
        0 * workspaceMemberRepository.findByWorkspaceIdAndUserId(_, _)
    }

    @Unroll
    def "addMembership - saves a new membership with the given role #role and records a MEMBER_JOINED audit entry"() {
        given:
        def user = User.builder().id(2L).email("a@b.com").build()
        def workspace = Workspace.builder().id(1L).build()
        workspaceMemberRepository.existsByWorkspaceIdAndUserId(1L, 2L) >> false
        workspaceRepository.findById(1L) >> Optional.of(workspace)

        when:
        service.addMembership(1L, user, role)

        then:
        1 * workspaceMemberRepository.save({ WorkspaceMember m -> m.role == role && m.user == user })
        1 * auditLogService.record(workspace, AuditAction.MEMBER_JOINED, user, null)

        where:
        role << [WorkspaceRole.COLLABORATOR, WorkspaceRole.READ_ONLY]
    }

    def "addMembership - throws EntityAlreadyExistsException when already a member"() {
        given:
        def user = User.builder().id(2L).build()
        workspaceMemberRepository.existsByWorkspaceIdAndUserId(1L, 2L) >> true

        when:
        service.addMembership(1L, user, WorkspaceRole.COLLABORATOR)

        then:
        thrown(EntityAlreadyExistsException)
    }

    def "addMembership - throws EntityNotFoundException when the workspace does not exist"() {
        given:
        def user = User.builder().id(2L).build()
        workspaceMemberRepository.existsByWorkspaceIdAndUserId(1L, 2L) >> false
        workspaceRepository.findById(1L) >> Optional.empty()

        when:
        service.addMembership(1L, user, WorkspaceRole.COLLABORATOR)

        then:
        thrown(EntityNotFoundException)
    }

    def "removeMembership - throws PermissionDeniedException when the actor tries to remove themselves"() {
        given:
        def actor = User.builder().id(2L).build()

        when:
        service.removeMembership(1L, actor, 2L)

        then:
        thrown(PermissionDeniedException)
        0 * workspaceMemberRepository.delete(_)
    }

    def "removeMembership - throws EntityNotFoundException when the target does not belong to the workspace"() {
        given:
        def actor = User.builder().id(2L).build()
        workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 3L) >> Optional.empty()

        when:
        service.removeMembership(1L, actor, 3L)

        then:
        thrown(EntityNotFoundException)
    }

    def "removeMembership - OWNER can remove a COLLABORATOR"() {
        given:
        def workspace = Workspace.builder().id(1L).build()
        def actor = User.builder().id(2L).email("owner@example.com").build()
        def actorMembership = WorkspaceMember.builder().id(10L).workspace(workspace).user(actor).role(WorkspaceRole.OWNER).build()
        def targetUser = User.builder().id(3L).email("target@example.com").build()
        def targetMembership = WorkspaceMember.builder().id(11L).workspace(workspace).user(targetUser).role(WorkspaceRole.COLLABORATOR).build()

        workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 3L) >> Optional.of(targetMembership)
        workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 2L) >> Optional.of(actorMembership)

        when:
        service.removeMembership(1L, actor, 3L)

        then:
        1 * workspaceMemberRepository.delete(targetMembership)
        1 * auditLogService.record(workspace, AuditAction.MEMBER_REMOVED, actor, targetUser)
        1 * workspaceMembershipEventPublisher.publishMemberRemoved({ MemberRemovedEvent e ->
            e.workspaceId() == 1L && e.removedByEmail() == "owner@example.com" && e.removedUserEmail() == "target@example.com"
        })
        0 * workspaceMemberRepository.save(_)
    }

    def "removeMembership - stamps the published event with the request's correlation id"() {
        given:
        def workspace = Workspace.builder().id(1L).build()
        def actor = User.builder().id(2L).email("owner@example.com").build()
        def actorMembership = WorkspaceMember.builder().id(10L).workspace(workspace).user(actor).role(WorkspaceRole.OWNER).build()
        def targetUser = User.builder().id(3L).email("target@example.com").build()
        def targetMembership = WorkspaceMember.builder().id(11L).workspace(workspace).user(targetUser).role(WorkspaceRole.COLLABORATOR).build()
        MDC.put("correlationId", "trace-456")

        workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 3L) >> Optional.of(targetMembership)
        workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 2L) >> Optional.of(actorMembership)

        when:
        service.removeMembership(1L, actor, 3L)

        then:
        1 * workspaceMembershipEventPublisher.publishMemberRemoved({ MemberRemovedEvent e -> e.correlationId() == "trace-456" })
    }

    def "removeMembership - a plain COLLABORATOR without ROLE_ADMIN cannot remove members"() {
        given:
        def workspace = Workspace.builder().id(1L).build()
        def actor = User.builder().id(2L).build()
        def actorMembership = WorkspaceMember.builder().id(10L).workspace(workspace).user(actor).role(WorkspaceRole.COLLABORATOR).build()
        def targetMembership = WorkspaceMember.builder().id(11L).workspace(workspace).user(User.builder().id(3L).build()).role(WorkspaceRole.COLLABORATOR).build()

        workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 3L) >> Optional.of(targetMembership)
        workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 2L) >> Optional.of(actorMembership)

        when:
        service.removeMembership(1L, actor, 3L)

        then:
        thrown(PermissionDeniedException)
        0 * workspaceMemberRepository.delete(_)
    }

    def "removeMembership - a global admin who is not a member can remove any member"() {
        given:
        def workspace = Workspace.builder().id(1L).build()
        def actor = User.builder().id(2L).userRoles([UserRole.ROLE_ADMIN] as Set).build()
        def targetMembership = WorkspaceMember.builder().id(11L).workspace(workspace).user(User.builder().id(3L).build()).role(WorkspaceRole.COLLABORATOR).build()

        workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 3L) >> Optional.of(targetMembership)
        workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 2L) >> Optional.empty()

        when:
        service.removeMembership(1L, actor, 3L)

        then:
        1 * workspaceMemberRepository.delete(targetMembership)
        0 * workspaceMemberRepository.save(_)
    }

    def "removeMembership - when an admin removes the OWNER, the admin becomes the new OWNER"() {
        given:
        def workspace = Workspace.builder().id(1L).build()
        def admin = User.builder().id(2L).userRoles([UserRole.ROLE_ADMIN] as Set).build()
        def ownerUser = User.builder().id(3L).build()
        def ownerMembership = WorkspaceMember.builder().id(11L).workspace(workspace).user(ownerUser).role(WorkspaceRole.OWNER).build()

        workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 3L) >> Optional.of(ownerMembership)
        workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 2L) >> Optional.empty()

        when:
        service.removeMembership(1L, admin, 3L)

        then:
        1 * workspaceMemberRepository.delete(ownerMembership)
        1 * workspaceMemberRepository.save({ WorkspaceMember m -> m.user == admin && m.role == WorkspaceRole.OWNER })
    }

    def "removeMembership - when an admin who is already a member removes the OWNER, their existing membership is promoted"() {
        given:
        def workspace = Workspace.builder().id(1L).build()
        def admin = User.builder().id(2L).userRoles([UserRole.ROLE_ADMIN] as Set).build()
        def adminMembership = WorkspaceMember.builder().id(10L).workspace(workspace).user(admin).role(WorkspaceRole.READ_ONLY).build()
        def ownerUser = User.builder().id(3L).build()
        def ownerMembership = WorkspaceMember.builder().id(11L).workspace(workspace).user(ownerUser).role(WorkspaceRole.OWNER).build()

        workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 3L) >> Optional.of(ownerMembership)
        workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 2L) >> Optional.of(adminMembership)

        when:
        service.removeMembership(1L, admin, 3L)

        then:
        1 * workspaceMemberRepository.delete(ownerMembership)
        1 * workspaceMemberRepository.save({ WorkspaceMember m -> m == adminMembership && m.role == WorkspaceRole.OWNER })
    }

    def "transferOwnership - OWNER transfers to a COLLABORATOR, who becomes OWNER while the actor is demoted"() {
        given:
        def workspace = Workspace.builder().id(1L).build()
        def actor = User.builder().id(2L).email("owner@example.com").build()
        def actorMembership = WorkspaceMember.builder().id(10L).workspace(workspace).user(actor).role(WorkspaceRole.OWNER).build()
        def newOwnerUser = User.builder().id(3L).email("newowner@example.com").build()
        def newOwnerMembership = WorkspaceMember.builder().id(11L).workspace(workspace).user(newOwnerUser).role(WorkspaceRole.COLLABORATOR).build()

        workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 2L) >> Optional.of(actorMembership)
        workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 3L) >> Optional.of(newOwnerMembership)
        workspaceMemberRepository.findByWorkspaceIdAndRole(1L, WorkspaceRole.OWNER) >> Optional.of(actorMembership)

        when:
        service.transferOwnership(1L, actor, 3L)

        then:
        1 * workspaceMemberRepository.save({ WorkspaceMember m -> m == newOwnerMembership && m.role == WorkspaceRole.OWNER })
        1 * workspaceMemberRepository.save({ WorkspaceMember m -> m == actorMembership && m.role == WorkspaceRole.COLLABORATOR })
        1 * auditLogService.record(workspace, AuditAction.OWNERSHIP_TRANSFERRED, actor, newOwnerUser)
    }

    def "transferOwnership - a global admin who is not the OWNER still demotes the actual current OWNER, not the admin"() {
        given:
        def workspace = Workspace.builder().id(1L).build()
        def admin = User.builder().id(2L).userRoles([UserRole.ROLE_ADMIN] as Set).build()
        def previousOwnerUser = User.builder().id(4L).build()
        def previousOwnerMembership = WorkspaceMember.builder().id(12L).workspace(workspace).user(previousOwnerUser).role(WorkspaceRole.OWNER).build()
        def newOwnerUser = User.builder().id(3L).build()
        def newOwnerMembership = WorkspaceMember.builder().id(11L).workspace(workspace).user(newOwnerUser).role(WorkspaceRole.COLLABORATOR).build()

        workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 2L) >> Optional.empty()
        workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 3L) >> Optional.of(newOwnerMembership)
        workspaceMemberRepository.findByWorkspaceIdAndRole(1L, WorkspaceRole.OWNER) >> Optional.of(previousOwnerMembership)

        when:
        service.transferOwnership(1L, admin, 3L)

        then:
        1 * workspaceMemberRepository.save({ WorkspaceMember m -> m == newOwnerMembership && m.role == WorkspaceRole.OWNER })
        1 * workspaceMemberRepository.save({ WorkspaceMember m -> m == previousOwnerMembership && m.role == WorkspaceRole.COLLABORATOR })
    }

    def "transferOwnership - does not re-save the new owner as its own previous-owner demotion target"() {
        given:
        def workspace = Workspace.builder().id(1L).build()
        def admin = User.builder().id(2L).userRoles([UserRole.ROLE_ADMIN] as Set).build()
        def newOwnerUser = User.builder().id(3L).build()
        def newOwnerMembership = WorkspaceMember.builder().id(11L).workspace(workspace).user(newOwnerUser).role(WorkspaceRole.OWNER).build()

        workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 2L) >> Optional.empty()
        workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 3L) >> Optional.of(newOwnerMembership)
        workspaceMemberRepository.findByWorkspaceIdAndRole(1L, WorkspaceRole.OWNER) >> Optional.of(newOwnerMembership)

        when:
        service.transferOwnership(1L, admin, 3L)

        then:
        1 * workspaceMemberRepository.save({ WorkspaceMember m -> m == newOwnerMembership && m.role == WorkspaceRole.OWNER })
        0 * workspaceMemberRepository.save({ WorkspaceMember m -> m.role == WorkspaceRole.COLLABORATOR })
    }

    def "transferOwnership - a plain COLLABORATOR without ROLE_ADMIN cannot transfer ownership"() {
        given:
        def workspace = Workspace.builder().id(1L).build()
        def actor = User.builder().id(2L).build()
        def actorMembership = WorkspaceMember.builder().id(10L).workspace(workspace).user(actor).role(WorkspaceRole.COLLABORATOR).build()

        workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 2L) >> Optional.of(actorMembership)

        when:
        service.transferOwnership(1L, actor, 3L)

        then:
        thrown(PermissionDeniedException)
        0 * workspaceMemberRepository.save(_)
    }

    def "transferOwnership - throws PermissionDeniedException when transferring to oneself"() {
        given:
        def workspace = Workspace.builder().id(1L).build()
        def actor = User.builder().id(2L).build()
        def actorMembership = WorkspaceMember.builder().id(10L).workspace(workspace).user(actor).role(WorkspaceRole.OWNER).build()

        workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 2L) >> Optional.of(actorMembership)

        when:
        service.transferOwnership(1L, actor, 2L)

        then:
        thrown(PermissionDeniedException)
        0 * workspaceMemberRepository.save(_)
    }

    def "transferOwnership - throws EntityNotFoundException when the new owner does not belong to the workspace"() {
        given:
        def workspace = Workspace.builder().id(1L).build()
        def actor = User.builder().id(2L).build()
        def actorMembership = WorkspaceMember.builder().id(10L).workspace(workspace).user(actor).role(WorkspaceRole.OWNER).build()

        workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 2L) >> Optional.of(actorMembership)
        workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 3L) >> Optional.empty()

        when:
        service.transferOwnership(1L, actor, 3L)

        then:
        thrown(EntityNotFoundException)
        0 * workspaceMemberRepository.save(_)
    }
}
