package com.api.identity.services.workspace;

import com.api.identity.entities.User;
import com.api.identity.entities.WorkspaceMember;
import com.api.identity.enums.WorkspaceRole;
import com.api.identity.exceptions.EntityAlreadyExistsException;
import com.api.identity.exceptions.EntityNotFoundException;
import com.api.identity.repositories.WorkspaceMemberRepository;
import com.api.identity.repositories.WorkspaceRepository;
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

    @Transactional(readOnly = true)
    public void verifyMembership(Long workspaceId, Long userId) {
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new EntityNotFoundException("El usuario no pertenece al workspace indicado");
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
        log.debug("Se agrego el usuario {} al workspace {}", user.getEmail(), workspaceId);
    }
}
