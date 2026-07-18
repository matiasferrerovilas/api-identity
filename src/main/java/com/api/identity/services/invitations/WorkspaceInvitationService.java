package com.api.identity.services.invitations;

import com.api.identity.entities.WorkspaceInvitation;
import com.api.identity.enums.InvitationStatus;
import com.api.identity.mappers.WorkspaceInvitationMapper;
import com.api.identity.records.invitations.AcceptRejectInvitationDTO;
import com.api.identity.records.workspaces.WorkspaceInvitationDTO;
import com.api.identity.records.workspaces.WorkspaceSendInvitationDTO;
import com.api.identity.repositories.WorkspaceInvitationRepository;
import com.api.identity.services.user.UserService;
import com.api.identity.services.workspace.WorkspaceMembershipService;
import com.api.identity.services.workspace.WorkspaceService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
    private final WorkspaceMembershipService workspaceMembershipService;
    private final WorkspaceService workspaceService;
    private final ApplicationEventPublisher  applicationEventPublisher;

    @Transactional(readOnly = true)
    public List<WorkspaceInvitationDTO> getPendingInvitations() {
        var user = userService.getAuthenticatedUser();

        return workspaceInvitationMapper.toDTO(
                workspaceInvitationRepository.findByInvitedUser_IdAndStatus(user.getId(), InvitationStatus.PENDING));
    }

    public void sendInvitation(Long workspaceId, @Valid WorkspaceSendInvitationDTO body) {
        var user = userService.getAuthenticatedUser();

        workspaceMembershipService.verifyMembership(workspaceId, user.getId());
        var workspaceToInvite = workspaceService.findWorkspaceById(workspaceId);

        userService.getUserByEmail(body.emails())
                .forEach(userInvited -> {
                    var pendingInvitations = workspaceInvitationRepository.findByWorkspace_IdAndStatusAndInvitedUser_Id(workspaceId, InvitationStatus.PENDING, userInvited.getId());

                    if(pendingInvitations.isPresent()){
                        log.error("Ya existen invitaciones pendientes a este workspace para este usuario");
                        return;
                    }

                    log.info("Enviando solicitud al worskpace {} al usuario de email {}", workspaceId, userInvited.getEmail());

                    var workspaceInvitation = workspaceInvitationRepository.save(WorkspaceInvitation.builder()
                            .invitedBy(user)
                            .invitedUser(userInvited)
                            .status(InvitationStatus.PENDING)
                            .workspace(workspaceToInvite)
                            .build());
                    ;
                    applicationEventPublisher.publishEvent(workspaceInvitationMapper.toDTO(workspaceInvitation));
                });

    }

    public void acceptRejectInvitation(@Valid AcceptRejectInvitationDTO invitationDTO) {
        var user = userService.getAuthenticatedUser();

        var invitation = workspaceInvitationRepository.findById(invitationDTO.id())
                .orElseThrow(EntityNotFoundException::new);

        if(!InvitationStatus.PENDING.equals(invitation.getStatus())){
            log.error("La Invitación ya fue rechazada/aceptada");
            return;
        }
        if(!invitation.getInvitedUser().getId().equals(user.getId())){
            log.error("El usuario no tiene Invitación para el solicitud");
            return;
        }
        invitation.setStatus(invitationDTO.status() ? InvitationStatus.ACCEPTED : InvitationStatus.REJECTED);
        workspaceInvitationRepository.save(invitation);
        if(invitationDTO.status()){
            workspaceMembershipService.addMembership(invitation.getWorkspace().getId(), user);
        }
        log.debug("Invitación {} actualizada correctamente a {}", invitationDTO.id(), invitation.getStatus());
    }
}