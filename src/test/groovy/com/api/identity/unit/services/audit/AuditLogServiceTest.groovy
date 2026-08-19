package com.api.identity.unit.services.audit

import com.api.identity.entities.AuditLog
import com.api.identity.entities.User
import com.api.identity.entities.Workspace
import com.api.identity.enums.AuditAction
import com.api.identity.mappers.AuditLogMapper
import com.api.identity.records.audit.AuditLogDTO
import com.api.identity.repositories.AuditLogRepository
import com.api.identity.services.audit.AuditLogService
import spock.lang.Specification

class AuditLogServiceTest extends Specification {

    AuditLogRepository auditLogRepository = Mock(AuditLogRepository)
    AuditLogMapper auditLogMapper = Mock(AuditLogMapper)

    AuditLogService service = new AuditLogService(auditLogRepository, auditLogMapper)

    def "record - persists an audit entry with the given actor, target and action"() {
        given:
        def workspace = Workspace.builder().id(1L).build()
        def actor = User.builder().id(2L).email("actor@example.com").build()
        def target = User.builder().id(3L).email("target@example.com").build()

        when:
        service.record(workspace, AuditAction.INVITATION_SENT, actor, target)

        then:
        1 * auditLogRepository.save({ AuditLog log ->
            log.workspace == workspace && log.action == AuditAction.INVITATION_SENT &&
                    log.actor == actor && log.targetUser == target
        })
    }

    def "record - allows a null target for self-referential actions like joining or leaving"() {
        given:
        def workspace = Workspace.builder().id(1L).build()
        def actor = User.builder().id(2L).build()

        when:
        service.record(workspace, AuditAction.MEMBER_JOINED, actor, null)

        then:
        1 * auditLogRepository.save({ AuditLog log -> log.targetUser == null })
    }

    def "getByWorkspace - maps repository results ordered by createdAt desc"() {
        given:
        def entries = [AuditLog.builder().id(1L).build()]
        auditLogRepository.findByWorkspaceIdOrderByCreatedAtDesc(10L) >> entries
        auditLogMapper.toDTO(entries) >> [new AuditLogDTO(1L, AuditAction.MEMBER_JOINED, "a@b.com", null, null)]

        when:
        def result = service.getByWorkspace(10L)

        then:
        result.size() == 1
        result[0].action() == AuditAction.MEMBER_JOINED
    }
}
