package com.api.identity.services.workspace;

import com.api.identity.exceptions.EntityNotFoundException;
import com.api.identity.repositories.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkspaceMembershipService {

    private final WorkspaceMemberRepository workspaceMemberRepository;

    @Transactional(readOnly = true)
    public void verifyMembership(Long workspaceId, Long userId) {
        if (!workspaceMemberRepository.existsByWorkspace_IdAndUser_Id(workspaceId, userId)) {
            throw new EntityNotFoundException("El usuario no pertenece al workspace indicado");
        }
    }
}
