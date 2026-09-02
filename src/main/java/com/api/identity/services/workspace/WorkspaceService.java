package com.api.identity.services.workspace;

import com.api.identity.entities.Workspace;
import com.api.identity.enums.AuditAction;
import com.api.identity.enums.WorkspaceRole;
import com.api.identity.exceptions.EntityNotFoundException;
import com.api.identity.mappers.AdminWorkspaceMapper;
import com.api.identity.mappers.WorkspaceMapper;
import com.api.identity.mappers.WorkspaceMemberMapper;
import com.api.identity.records.admin.AdminWorkspaceSummaryDTO;
import com.api.identity.records.audit.AuditLogDTO;
import com.api.identity.records.workspaces.WorkspaceDTO;
import com.api.identity.records.workspaces.WorkspaceMemberDTO;
import com.api.identity.repositories.WorkspaceMemberRepository;
import com.api.identity.repositories.WorkspaceRepository;
import com.api.identity.services.audit.AuditLogService;
import com.api.identity.services.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMapper workspaceMapper;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceMemberMapper workspaceMemberMapper;
    private final AdminWorkspaceMapper adminWorkspaceMapper;
    private final UserService userService;
    private final WorkspaceMembershipService workspaceMembershipService;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<WorkspaceMemberDTO> getWorkspaceMembers() {
        var user = userService.getAuthenticatedUser();

        return workspaceMemberMapper.toDTO(
                workspaceMemberRepository.findByWorkspaceOwnerOrMember(user.getId()), user.getId());
    }

    @Transactional(readOnly = true)
    public WorkspaceMemberDTO getWorkspaceMembers(Long workspaceId) {
        var user = userService.getAuthenticatedUser();
        workspaceMembershipService.verifyMembership(workspaceId, user.getId());

        return workspaceMemberMapper.toDTO(
                        workspaceMemberRepository.findByWorkspaceId(workspaceId), user.getId())
                .getFirst();
    }

    @Transactional
    public void leaveWorkspace(Long workspaceId) {
        var owner = userService.getAuthenticatedUser();
        var membership = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, owner.getId())
                        .orElseThrow(() -> new EntityNotFoundException("El usuario no pertenece al workspace indicado"));

        var otherMembers = workspaceMemberRepository.findByWorkspaceId(workspaceId).stream()
                .filter(m -> !m.getUser().getId().equals(owner.getId()))
                .toList();

        if (membership.getRole() == WorkspaceRole.OWNER) {
            otherMembers.stream()
                    .findFirst()
                    .ifPresent(newOwner -> {
                        newOwner.setRole(WorkspaceRole.OWNER);
                        workspaceMemberRepository.save(newOwner);
                    });
        }

        workspaceMemberRepository.delete(membership);
        auditLogService.record(membership.getWorkspace(), AuditAction.MEMBER_LEFT, owner, null);
        log.info("Usuario {} salió del workspace {}", owner.getId(), workspaceId);

        if (otherMembers.isEmpty()) {
            var workspace = membership.getWorkspace();
            workspace.setActive(false);
            workspaceRepository.save(workspace);
            log.info("Workspace {} se quedó sin miembros y fue desactivado", workspaceId);
        }
    }

    @Transactional
    public void removeMember(Long workspaceId, Long targetUserId) {
        var actor = userService.getAuthenticatedUser();
        workspaceMembershipService.removeMembership(workspaceId, actor, targetUserId);
    }

    @Transactional
    public void transferOwnership(Long workspaceId, Long newOwnerUserId) {
        var actor = userService.getAuthenticatedUser();
        workspaceMembershipService.transferOwnership(workspaceId, actor, newOwnerUserId);
    }

    @Transactional
    public void changeMemberRole(Long workspaceId, Long targetUserId, WorkspaceRole newRole) {
        var actor = userService.getAuthenticatedUser();
        workspaceMembershipService.changeRole(workspaceId, actor, targetUserId, newRole);
    }

    @Transactional(readOnly = true)
    public List<AuditLogDTO> getWorkspaceAuditLog(Long workspaceId) {
        var user = userService.getAuthenticatedUser();
        workspaceMembershipService.verifyCanViewAuditLog(workspaceId, user);

        return auditLogService.getByWorkspace(workspaceId);
    }

    /** Listado admin-wide: TODOS los workspaces activos de la instancia, cada uno con sus
     * miembros y el rol de cada uno — la contracara de
     * {@link UserService#listAllUsersWithWorkspaces()}. Protegido a nivel de
     * SecurityConfiguration ({@code /v1/admin/**} → ROLE_ADMIN), no acá — ver AdminController. */
    @Transactional(readOnly = true)
    public List<AdminWorkspaceSummaryDTO> listAllWorkspacesWithMembers() {
        var membersByWorkspaceId = workspaceMemberRepository.findAllActiveWithWorkspaceAndUser().stream()
                .collect(Collectors.groupingBy(m -> m.getWorkspace().getId()));

        return workspaceRepository.findAllByIsActiveTrue().stream()
                .map(workspace -> adminWorkspaceMapper.toAdminWorkspaceSummaryDTO(
                        workspace,
                        adminWorkspaceMapper.toMemberSummaries(
                                membersByWorkspaceId.getOrDefault(workspace.getId(), Collections.emptyList()))))
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkspaceDTO getWorkspaceDTOById(Long workspaceId) {
        var user = userService.getAuthenticatedUser();
        // Sin este chequeo, cualquier usuario autenticado podía pasar cualquier workspaceId y
        // recibir el owner + la lista completa de emails de los miembros de un workspace ajeno —
        // a diferencia de cada endpoint hermano en WorkspaceController, este no verificaba
        // membership antes de devolver datos.
        workspaceMembershipService.verifyMembership(workspaceId, user.getId());

        return workspaceMapper.toDTO(
                workspaceRepository.findById(workspaceId)
                        .orElseThrow(() -> new EntityNotFoundException("Workspace no encontrado")));
    }
    @Transactional(readOnly = true)
    public Workspace findWorkspaceById(Long workspaceId) {
        return workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new EntityNotFoundException("Workspace no encontrado"));
    }


}
