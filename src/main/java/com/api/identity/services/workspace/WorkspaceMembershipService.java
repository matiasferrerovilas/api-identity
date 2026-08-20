package com.api.identity.services.workspace;

import com.api.identity.entities.User;
import com.api.identity.entities.WorkspaceMember;
import com.api.identity.enums.AuditAction;
import com.api.identity.enums.UserRole;
import com.api.identity.enums.WorkspaceRole;
import com.api.identity.events.MemberRemovedEvent;
import com.api.identity.exceptions.EntityAlreadyExistsException;
import com.api.identity.exceptions.EntityNotFoundException;
import com.api.identity.exceptions.PermissionDeniedException;
import com.api.identity.repositories.WorkspaceMemberRepository;
import com.api.identity.repositories.WorkspaceRepository;
import com.api.identity.services.audit.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceMembershipService {

    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceRepository workspaceRepository;
    private final AuditLogService auditLogService;
    private final WorkspaceMembershipEventPublisher workspaceMembershipEventPublisher;

    @Transactional(readOnly = true)
    public void verifyMembership(Long workspaceId, Long userId) {
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new EntityNotFoundException("El usuario no pertenece al workspace indicado");
        }
    }

    @Transactional(readOnly = true)
    public void verifyCanInvite(Long workspaceId, Long userId) {
        this.requireAtLeastCollaborator(workspaceId, userId,
                "Los miembros de solo lectura no pueden invitar a otros usuarios");
    }

    @Transactional(readOnly = true)
    public void verifyCanViewAuditLog(Long workspaceId, Long userId) {
        this.requireAtLeastCollaborator(workspaceId, userId,
                "Los miembros de solo lectura no pueden ver la auditoría del workspace");
    }

    private void requireAtLeastCollaborator(Long workspaceId, Long userId, String deniedMessage) {
        WorkspaceRole role = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new EntityNotFoundException("El usuario no pertenece al workspace indicado"))
                .getRole();

        if (role == WorkspaceRole.READ_ONLY) {
            throw new PermissionDeniedException(deniedMessage);
        }
    }

    public void addMembership(Long workspaceId, User user) {
        if (workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, user.getId())) {
            throw new EntityAlreadyExistsException("El usuario ya pertenece al workspace indicado");
        }
        var workspace = workspaceRepository.findById(workspaceId).orElseThrow(() -> new EntityNotFoundException("El workspace no existe"));
        workspaceMemberRepository.save(WorkspaceMember.builder()
                .workspace(workspace)
                        .joinedAt(LocalDateTime.now())
                        .user(user)
                        .role(WorkspaceRole.COLLABORATOR)
                .build());
        auditLogService.record(workspace, AuditAction.MEMBER_JOINED, user, null);
        log.debug("Se agrego el usuario {} al workspace {}", user.getEmail(), workspaceId);
    }

    public void removeMembership(Long workspaceId, User actor, Long targetUserId) {
        if (actor.getId().equals(targetUserId)) {
            throw new PermissionDeniedException("No podés eliminarte a vos mismo del workspace; usá el endpoint para salir del workspace");
        }

        var targetMembership = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, targetUserId)
                .orElseThrow(() -> new EntityNotFoundException("El usuario no pertenece al workspace indicado"));

        boolean isAdmin = actor.getUserRoles().contains(UserRole.ROLE_ADMIN);
        var actorMembership = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, actor.getId());
        boolean isOwner = actorMembership.map(m -> m.getRole() == WorkspaceRole.OWNER).orElse(false);

        if (!isOwner && !isAdmin) {
            throw new PermissionDeniedException("Solo el OWNER del workspace o un administrador pueden eliminar miembros");
        }

        var workspace = targetMembership.getWorkspace();
        boolean removingOwner = targetMembership.getRole() == WorkspaceRole.OWNER;

        workspaceMemberRepository.delete(targetMembership);
        auditLogService.record(workspace, AuditAction.MEMBER_REMOVED, actor, targetMembership.getUser());
        workspaceMembershipEventPublisher.publishMemberRemoved(new MemberRemovedEvent(
                workspace.getId(), workspace.getName(), actor.getEmail(),
                targetMembership.getUser().getEmail(), LocalDateTime.now()));
        log.info("Usuario {} eliminó a {} del workspace {}", actor.getEmail(), targetMembership.getUser().getEmail(), workspaceId);

        if (removingOwner && isAdmin) {
            if (actorMembership.isPresent()) {
                var membership = actorMembership.get();
                membership.setRole(WorkspaceRole.OWNER);
                workspaceMemberRepository.save(membership);
            } else {
                workspaceMemberRepository.save(WorkspaceMember.builder()
                        .workspace(workspace)
                        .user(actor)
                        .role(WorkspaceRole.OWNER)
                        .joinedAt(LocalDateTime.now())
                        .build());
            }
            log.info("El administrador {} pasó a ser OWNER del workspace {} tras eliminar al owner anterior", actor.getEmail(), workspaceId);
        }
    }
}
