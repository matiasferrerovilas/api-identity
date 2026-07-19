package com.api.identity.services.workspace;

import com.api.identity.entities.Workspace;
import com.api.identity.enums.WorkspaceRole;
import com.api.identity.exceptions.EntityNotFoundException;
import com.api.identity.mappers.WorkspaceMapper;
import com.api.identity.mappers.WorkspaceMemberMapper;
import com.api.identity.records.workspaces.WorkspaceDTO;
import com.api.identity.records.workspaces.WorkspaceMemberDTO;
import com.api.identity.repositories.WorkspaceMemberRepository;
import com.api.identity.repositories.WorkspaceRepository;
import com.api.identity.services.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMapper workspaceMapper;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceMemberMapper workspaceMemberMapper;
    private final UserService userService;
    private final WorkspaceMembershipService workspaceMembershipService;

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
    public void deleteWorkspace(Long workspaceId) {
        var owner = userService.getAuthenticatedUser();
        var membership = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, owner.getId())
                        .orElseThrow(() -> new EntityNotFoundException("El usuario no pertenece al workspace indicado"));

        if (membership.getRole() == WorkspaceRole.OWNER) {
            workspaceMemberRepository.findByWorkspaceId(workspaceId).stream()
                    .filter(m -> !m.getUser().getId().equals(owner.getId()))
                    .findFirst()
                    .ifPresent(newOwner -> {
                        newOwner.setRole(WorkspaceRole.OWNER);
                        workspaceMemberRepository.save(newOwner);
                    });
        }

        workspaceMemberRepository.delete(membership);
        log.info("Usuario {} salió del workspace {}", owner.getId(), workspaceId);
    }

    @Transactional(readOnly = true)
    public WorkspaceDTO getWorkspaceDTOById(Long workspaceId) {
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
