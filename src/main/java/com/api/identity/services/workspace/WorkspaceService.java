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
                        workspaceMemberRepository.findByWorkspace_Id(workspaceId), user.getId())
                .getFirst();
    }

    @Transactional
    public void deleteWorkspace(Long workspaceId) {
        var owner = userService.getAuthenticatedUser();
        workspaceMembershipService.verifyMembership(workspaceId, owner.getId());
        var membership = workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspaceId, owner.getId())
                        .orElseThrow(() -> new EntityNotFoundException("No existe el workspace con id " + workspaceId));
        log.info("Deleting workspace {}", workspaceId);

        if(membership.getRole().equals(WorkspaceRole.OWNER)){
            var workspaceToBeRemoved = this.getWorkspaceDTOById(workspaceId);
            if(workspaceToBeRemoved.metadata().members().size() > 1){
                var anotherUserEmail = workspaceToBeRemoved.metadata().members().getLast();
                var anotherUserId = userService.getUserByEmail(List.of(anotherUserEmail))
                        .getFirst()
                        .getId();
                var anotherMembership = workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspaceId, anotherUserId)
                        .orElseThrow(() -> new EntityNotFoundException("No existe el workspace con id " + workspaceId));
                anotherMembership.setRole(WorkspaceRole.OWNER);
                workspaceMemberRepository.save(anotherMembership);
                workspaceMemberRepository.delete(membership);
            }
        }
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
