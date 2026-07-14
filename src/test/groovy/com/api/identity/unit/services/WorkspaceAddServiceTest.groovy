package com.api.identity.unit.services

import com.api.identity.entities.User
import com.api.identity.entities.Workspace
import com.api.identity.enums.WorkspaceRole
import com.api.identity.exceptions.BusinessException
import com.api.identity.exceptions.EntityNotFoundException
import com.api.identity.records.workspaces.AddWorkspaceRecord
import com.api.identity.repositories.UserRepository
import com.api.identity.repositories.WorkspaceRepository
import com.api.identity.services.WorkspaceAddService
import spock.lang.Specification

class WorkspaceAddServiceTest extends Specification {

    UserRepository userRepository = Mock(UserRepository)
    WorkspaceRepository workspaceRepository = Mock(WorkspaceRepository)

    WorkspaceAddService service

    def setup() {
        service = new WorkspaceAddService(userRepository, workspaceRepository)
    }

    def "createWorkspaces - should save workspaces with owner membership and return WorkspaceAdded list"() {
        given:
        def owner = User.builder().id(1L).email("user@test.com").build()
        def workspacesToAdd = [new AddWorkspaceRecord("DEFAULT"), new AddWorkspaceRecord("Viajes")]

        userRepository.findById(1L) >> Optional.of(owner)
        workspaceRepository.findByOwnerIdAndNameIn(1L, ["DEFAULT", "Viajes"]) >> []

        when:
        def result = service.createWorkspaces(1L, workspacesToAdd)

        then:
        1 * workspaceRepository.saveAll(_ as List) >> { List args ->
            List<Workspace> workspaces = args[0] as List<Workspace>
            assert workspaces.size() == 2
            workspaces.eachWithIndex { workspace, i ->
                assert workspace.owner == owner
                assert workspace.members.size() == 1
                def member = workspace.members.first()
                assert member.user == owner
                assert member.role == WorkspaceRole.OWNER
                workspace.id = (i + 1) * 10L
            }
            return workspaces
        }
        result.size() == 2
        result*.description() == ["DEFAULT", "Viajes"]
        result*.id() == [10L, 20L]
    }

    def "createWorkspaces - should trim workspace names before saving"() {
        given:
        def owner = User.builder().id(1L).email("user@test.com").build()

        userRepository.findById(1L) >> Optional.of(owner)
        workspaceRepository.findByOwnerIdAndNameIn(1L, ["Casa"]) >> []
        workspaceRepository.saveAll(_ as List) >> { List args -> args[0] as List<Workspace> }

        when:
        def result = service.createWorkspaces(1L, [new AddWorkspaceRecord("  Casa  ")])

        then:
        result*.description() == ["Casa"]
    }

    def "createWorkspaces - should throw BusinessException when list is empty"() {
        when:
        service.createWorkspaces(1L, [])

        then:
        thrown(BusinessException)
        0 * workspaceRepository.saveAll(_ as List)
    }

    def "createWorkspaces - should throw EntityNotFoundException when user does not exist"() {
        given:
        userRepository.findById(99L) >> Optional.empty()

        when:
        service.createWorkspaces(99L, [new AddWorkspaceRecord("DEFAULT")])

        then:
        thrown(EntityNotFoundException)
        0 * workspaceRepository.saveAll(_ as List)
    }

    def "createWorkspaces - should throw BusinessException when description is blank"() {
        given:
        def owner = User.builder().id(1L).email("user@test.com").build()
        userRepository.findById(1L) >> Optional.of(owner)

        when:
        service.createWorkspaces(1L, [new AddWorkspaceRecord("   ")])

        then:
        thrown(BusinessException)
        0 * workspaceRepository.saveAll(_ as List)
    }

    def "createWorkspaces - should throw BusinessException when request has duplicated names"() {
        given:
        def owner = User.builder().id(1L).email("user@test.com").build()
        userRepository.findById(1L) >> Optional.of(owner)

        when:
        service.createWorkspaces(1L, [new AddWorkspaceRecord("Casa"), new AddWorkspaceRecord("Casa")])

        then:
        thrown(BusinessException)
        0 * workspaceRepository.saveAll(_ as List)
    }

    def "createWorkspaces - should throw BusinessException when a workspace name already exists for the owner"() {
        given:
        def owner = User.builder().id(1L).email("user@test.com").build()
        def existing = Workspace.builder().id(5L).name("Casa").owner(owner).build()

        userRepository.findById(1L) >> Optional.of(owner)
        workspaceRepository.findByOwnerIdAndNameIn(1L, ["Casa"]) >> [existing]

        when:
        service.createWorkspaces(1L, [new AddWorkspaceRecord("Casa")])

        then:
        thrown(BusinessException)
        0 * workspaceRepository.saveAll(_ as List)
    }
}
