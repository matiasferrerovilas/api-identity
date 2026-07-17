package com.api.identity.services.workspace;

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

    @Transactional(readOnly = true)
    public List<WorkspaceMemberDTO> getWorkspaceMembers() {
        var user = userService.getAuthenticatedUser();

        return workspaceMemberMapper.toDTO(
                workspaceMemberRepository.findByWorkspaceOwnerOrMember(user.getId()));
    }

    @Transactional(readOnly = true)
    public void verifyMembership(Long workspaceId, Long userId) {
        if (!workspaceMemberRepository.existsByWorkspace_IdAndUser_Id(workspaceId, userId)) {
            throw new EntityNotFoundException("El usuario no pertenece al workspace indicado");
        }
    }

    @Transactional(readOnly = true)
    public WorkspaceDTO getWorkspaceById(Long workspaceId) {
        return workspaceMapper.toDTO(
                workspaceRepository.findById(workspaceId)
                        .orElseThrow(() -> new EntityNotFoundException("Workspace no encontrado")));
    }
}
