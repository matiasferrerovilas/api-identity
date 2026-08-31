package com.api.identity.unit.mappers

import com.api.identity.entities.User
import com.api.identity.entities.Workspace
import com.api.identity.entities.WorkspaceMember
import com.api.identity.enums.WorkspaceRole
import com.api.identity.mappers.WorkspaceMemberMapper
import spock.lang.Specification

import java.time.LocalDateTime

class WorkspaceMemberMapperTest extends Specification {

    WorkspaceMemberMapper mapper = new WorkspaceMemberMapper()

    def "toDTO - includes memberDetails with userId, email and role for every member"() {
        given:
        def workspace = Workspace.builder().id(1L).name("Casa").build()
        def owner = User.builder().id(10L).email("owner@example.com").build()
        def collaborator = User.builder().id(11L).email("collab@example.com").build()
        def ownerMembership = WorkspaceMember.builder()
                .id(100L).workspace(workspace).user(owner).role(WorkspaceRole.OWNER).joinedAt(LocalDateTime.now()).build()
        def collaboratorMembership = WorkspaceMember.builder()
                .id(101L).workspace(workspace).user(collaborator).role(WorkspaceRole.COLLABORATOR).joinedAt(LocalDateTime.now()).build()

        when:
        def result = mapper.toDTO([ownerMembership, collaboratorMembership], 10L)

        then:
        result.size() == 1
        def dto = result[0]
        dto.metadata().memberDetails().size() == 2
        dto.metadata().memberDetails()[0] == new com.api.identity.records.workspaces.WorkspaceMemberDTO.MemberDetail(10L, "owner@example.com", WorkspaceRole.OWNER)
        dto.metadata().memberDetails()[1] == new com.api.identity.records.workspaces.WorkspaceMemberDTO.MemberDetail(11L, "collab@example.com", WorkspaceRole.COLLABORATOR)
        dto.metadata().role() == WorkspaceRole.OWNER
    }

    def "toDTO - resolves the calling user's own role even when they're not first in the list"() {
        given:
        def workspace = Workspace.builder().id(1L).name("Casa").build()
        def owner = User.builder().id(10L).email("owner@example.com").build()
        def caller = User.builder().id(12L).email("caller@example.com").build()
        def ownerMembership = WorkspaceMember.builder()
                .id(100L).workspace(workspace).user(owner).role(WorkspaceRole.OWNER).build()
        def callerMembership = WorkspaceMember.builder()
                .id(102L).workspace(workspace).user(caller).role(WorkspaceRole.READ_ONLY).build()

        when:
        def result = mapper.toDTO([ownerMembership, callerMembership], 12L)

        then:
        result[0].metadata().role() == WorkspaceRole.READ_ONLY
    }
}
