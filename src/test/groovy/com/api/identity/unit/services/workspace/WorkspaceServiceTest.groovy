package com.api.identity.unit.services.workspace

import com.api.identity.entities.User
import com.api.identity.entities.Workspace
import com.api.identity.entities.WorkspaceMember
import com.api.identity.enums.AuditAction
import com.api.identity.enums.WorkspaceRole
import com.api.identity.exceptions.EntityNotFoundException
import com.api.identity.mappers.WorkspaceMapper
import com.api.identity.mappers.WorkspaceMemberMapper
import com.api.identity.records.audit.AuditLogDTO
import com.api.identity.records.workspaces.WorkspaceDTO
import com.api.identity.repositories.WorkspaceMemberRepository
import com.api.identity.repositories.WorkspaceRepository
import com.api.identity.services.audit.AuditLogService
import com.api.identity.services.user.UserService
import com.api.identity.services.workspace.WorkspaceMembershipService
import com.api.identity.services.workspace.WorkspaceService
import spock.lang.Specification

class WorkspaceServiceTest extends Specification {

    WorkspaceRepository workspaceRepository = Mock(WorkspaceRepository)
    WorkspaceMapper workspaceMapper = Mock(WorkspaceMapper)
    WorkspaceMemberRepository workspaceMemberRepository = Mock(WorkspaceMemberRepository)
    WorkspaceMemberMapper workspaceMemberMapper = Mock(WorkspaceMemberMapper)
    UserService userService = Mock(UserService)
    WorkspaceMembershipService workspaceMembershipService = Mock(WorkspaceMembershipService)
    AuditLogService auditLogService = Mock(AuditLogService)

    WorkspaceService service = new WorkspaceService(
            workspaceRepository,
            workspaceMapper,
            workspaceMemberRepository,
            workspaceMemberMapper,
            userService,
            workspaceMembershipService,
            auditLogService)

    def owner = User.builder().id(1L).email("owner@example.com").build()
    def workspace = Workspace.builder().id(10L).name("Casa").build()

    def "leaveWorkspace - removes the membership and records a MEMBER_LEFT audit entry"() {
        given:
        def membership = WorkspaceMember.builder()
                .id(5L).workspace(workspace).user(owner).role(WorkspaceRole.COLLABORATOR).build()
        def other = WorkspaceMember.builder()
                .id(6L).workspace(workspace).user(User.builder().id(2L).build()).role(WorkspaceRole.COLLABORATOR).build()
        userService.getAuthenticatedUser() >> owner
        workspaceMemberRepository.findByWorkspaceIdAndUserId(10L, 1L) >> Optional.of(membership)
        workspaceMemberRepository.findByWorkspaceId(10L) >> [membership, other]

        when:
        service.leaveWorkspace(10L)

        then:
        1 * workspaceMemberRepository.delete(membership)
        1 * auditLogService.record(workspace, AuditAction.MEMBER_LEFT, owner, null)
        0 * workspaceRepository.save(_)
    }

    def "leaveWorkspace - transfers ownership to another member before the owner leaves"() {
        given:
        def membership = WorkspaceMember.builder()
                .id(5L).workspace(workspace).user(owner).role(WorkspaceRole.OWNER).build()
        def other = WorkspaceMember.builder()
                .id(6L).workspace(workspace).user(User.builder().id(2L).build()).role(WorkspaceRole.COLLABORATOR).build()
        userService.getAuthenticatedUser() >> owner
        workspaceMemberRepository.findByWorkspaceIdAndUserId(10L, 1L) >> Optional.of(membership)
        workspaceMemberRepository.findByWorkspaceId(10L) >> [membership, other]

        when:
        service.leaveWorkspace(10L)

        then:
        1 * workspaceMemberRepository.save({ WorkspaceMember m -> m == other && m.role == WorkspaceRole.OWNER })
        1 * workspaceMemberRepository.delete(membership)
        0 * workspaceRepository.save(_)
    }

    def "leaveWorkspace - deactivates the workspace when the last member leaves"() {
        given:
        def membership = WorkspaceMember.builder()
                .id(5L).workspace(workspace).user(owner).role(WorkspaceRole.OWNER).build()
        userService.getAuthenticatedUser() >> owner
        workspaceMemberRepository.findByWorkspaceIdAndUserId(10L, 1L) >> Optional.of(membership)
        workspaceMemberRepository.findByWorkspaceId(10L) >> [membership]

        when:
        service.leaveWorkspace(10L)

        then:
        1 * workspaceMemberRepository.delete(membership)
        0 * workspaceMemberRepository.save(_)
        1 * workspaceRepository.save({ Workspace w -> w == workspace && !w.active })
    }

    def "removeMember - delegates to WorkspaceMembershipService with the authenticated actor"() {
        given:
        userService.getAuthenticatedUser() >> owner

        when:
        service.removeMember(10L, 3L)

        then:
        1 * workspaceMembershipService.removeMembership(10L, owner, 3L)
    }

    def "transferOwnership - delegates to WorkspaceMembershipService with the authenticated actor"() {
        given:
        userService.getAuthenticatedUser() >> owner

        when:
        service.transferOwnership(10L, 3L)

        then:
        1 * workspaceMembershipService.transferOwnership(10L, owner, 3L)
    }

    def "getWorkspaceAuditLog - verifies permission before returning the log"() {
        given:
        userService.getAuthenticatedUser() >> owner
        auditLogService.getByWorkspace(10L) >> [
                new AuditLogDTO(1L, AuditAction.MEMBER_JOINED, "owner@example.com", null, null)
        ]

        when:
        def result = service.getWorkspaceAuditLog(10L)

        then:
        1 * workspaceMembershipService.verifyCanViewAuditLog(10L, 1L)
        result.size() == 1
    }

    def "getWorkspaceDTOById - verifies membership before returning the workspace"() {
        given:
        def dto = new WorkspaceDTO(10L, "Casa", "owner@example.com", new WorkspaceDTO.Metadata(["owner@example.com"]))
        userService.getAuthenticatedUser() >> owner
        workspaceRepository.findById(10L) >> Optional.of(workspace)
        workspaceMapper.toDTO(workspace) >> dto

        when:
        def result = service.getWorkspaceDTOById(10L)

        then:
        1 * workspaceMembershipService.verifyMembership(10L, 1L)
        result == dto
    }

    def "getWorkspaceDTOById - throws EntityNotFoundException when the user is not a member (IDOR fix)"() {
        given:
        userService.getAuthenticatedUser() >> owner
        workspaceMembershipService.verifyMembership(10L, 1L) >> {
            throw new EntityNotFoundException("El usuario no pertenece al workspace indicado")
        }

        when:
        service.getWorkspaceDTOById(10L)

        then:
        thrown(EntityNotFoundException)
        0 * workspaceRepository.findById(_)
    }
}
