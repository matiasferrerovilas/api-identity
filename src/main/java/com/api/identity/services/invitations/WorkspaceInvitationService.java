package com.api.identity.services.invitations;

import com.api.identity.enums.InvitationStatus;
import com.api.identity.mappers.WorkspaceInvitationMapper;
import com.api.identity.records.workspaces.WorkspaceInvitationDTO;
import com.api.identity.repositories.WorkspaceInvitationRepository;
import com.api.identity.services.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceInvitationService {

    private final WorkspaceInvitationRepository workspaceInvitationRepository;
    private final WorkspaceInvitationMapper workspaceInvitationMapper;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<WorkspaceInvitationDTO> getPendingInvitations() {
        var user = userService.getAuthenticatedUser();

        return workspaceInvitationMapper.toDTO(
                workspaceInvitationRepository.findByInvitedUser_IdAndStatus(user.getId(), InvitationStatus.PENDING));
    }
}
