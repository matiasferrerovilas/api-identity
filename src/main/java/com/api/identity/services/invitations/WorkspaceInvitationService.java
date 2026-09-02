package com.api.identity.services.invitations;

import com.api.identity.entities.WorkspaceInvitation;
import com.api.identity.enums.AuditAction;
import com.api.identity.enums.InvitationStatus;
import com.api.identity.enums.WorkspaceRole;
import com.api.identity.events.InvitationAcceptedEvent;
import com.api.identity.events.InvitationCreatedEvent;
import com.api.identity.exceptions.BusinessException;
import com.api.identity.exceptions.EntityNotFoundException;
import com.api.identity.exceptions.RateLimitExceededException;
import com.api.identity.logging.CorrelationIdHolder;
import com.api.identity.mappers.WorkspaceInvitationMapper;
import com.api.identity.records.invitations.AcceptRejectInvitationDTO;
import com.api.identity.records.workspaces.WorkspaceInvitationDTO;
import com.api.identity.records.workspaces.WorkspaceSendInvitationDTO;
import com.api.identity.records.workspaces.WorkspaceSentInvitationDTO;
import com.api.identity.repositories.WorkspaceInvitationRepository;
import com.api.identity.services.audit.AuditLogService;
import com.api.identity.services.ratelimit.RateLimiterService;
import com.api.identity.services.user.UserService;
import com.api.identity.services.workspace.WorkspaceMembershipService;
import com.api.identity.services.workspace.WorkspaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceInvitationService {

    // Un usuario legítimo manda invitaciones en tandas ocasionales, no en loop — 10 tandas por
    // hora deja margen de sobra para uso normal y corta un script que intente floodear emails.
    private static final int MAX_INVITATION_BATCHES_PER_HOUR = 10;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofHours(1);

    private final WorkspaceInvitationRepository workspaceInvitationRepository;
    private final WorkspaceInvitationMapper workspaceInvitationMapper;
    private final UserService userService;
    private final WorkspaceMembershipService workspaceMembershipService;
    private final WorkspaceService workspaceService;
    private final InvitationEventPublisher invitationEventPublisher;
    private final RateLimiterService rateLimiterService;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<WorkspaceInvitationDTO> getPendingInvitations() {
        var user = userService.getAuthenticatedUser();

        return workspaceInvitationMapper.toDTO(
                workspaceInvitationRepository.findByInvitedUserIdAndStatus(user.getId(), InvitationStatus.PENDING));
    }

    public void sendInvitation(Long workspaceId, @Valid WorkspaceSendInvitationDTO body) {
        var user = userService.getAuthenticatedUser();

        if (body.role() == WorkspaceRole.OWNER) {
            throw new BusinessException("No se puede invitar directamente como OWNER");
        }

        var rateLimitKey = "rate-limit:invitations:" + user.getId();
        if (!rateLimiterService.tryAcquire(rateLimitKey, MAX_INVITATION_BATCHES_PER_HOUR, RATE_LIMIT_WINDOW)) {
            throw new RateLimitExceededException(
                    "Alcanzaste el límite de invitaciones enviadas. Probá de nuevo más tarde.");
        }

        workspaceMembershipService.verifyCanInvite(workspaceId, user);
        var workspaceToInvite = workspaceService.findWorkspaceById(workspaceId);

        userService.getUserByEmail(body.emails())
                .forEach(userInvited -> {
                    var pendingInvitations = workspaceInvitationRepository.findByWorkspaceIdAndStatusAndInvitedUserId(
                            workspaceId, InvitationStatus.PENDING, userInvited.getId());

                    if (pendingInvitations.isPresent()) {
                        log.error("Ya existen invitaciones pendientes a este workspace para este usuario");
                        return;
                    }

                    log.info("Enviando solicitud al worskpace {} al usuario de email {}", workspaceId, userInvited.getEmail());

                    var workspaceInvitation = workspaceInvitationRepository.save(WorkspaceInvitation.builder()
                            .invitedBy(user)
                            .invitedUser(userInvited)
                            .status(InvitationStatus.PENDING)
                            .workspace(workspaceToInvite)
                            .role(body.role())
                            .build());
                    auditLogService.record(workspaceToInvite, AuditAction.INVITATION_SENT, user, userInvited);
                    invitationEventPublisher.publishInvitationCreated(new InvitationCreatedEvent(
                            workspaceInvitation.getId(),
                            workspaceToInvite.getId(),
                            workspaceToInvite.getName(),
                            user.getEmail(),
                            userInvited.getEmail(),
                            workspaceInvitation.getRole(),
                            workspaceInvitation.getCreatedAt(),
                            CorrelationIdHolder.current()));
                });

    }

    @Transactional(readOnly = true)
    public List<WorkspaceSentInvitationDTO> getSentInvitations() {
        var user = userService.getAuthenticatedUser();

        return workspaceInvitationMapper.toSentDTO(
                workspaceInvitationRepository.findByInvitedByIdOrderByCreatedAtDesc(user.getId()));
    }

    @Transactional
    public void cancelInvitation(Long invitationId) {
        var user = userService.getAuthenticatedUser();

        var invitation = workspaceInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new EntityNotFoundException("Invitación inexistente"));

        if (!invitation.getInvitedBy().getId().equals(user.getId())) {
            log.warn("El usuario {} intentó cancelar una invitación que no envió (id {})", user.getId(), invitationId);
            throw new EntityNotFoundException("Invitación inexistente");
        }
        if (!InvitationStatus.PENDING.equals(invitation.getStatus())) {
            throw new BusinessException("Solo se pueden cancelar invitaciones pendientes");
        }

        invitation.setStatus(InvitationStatus.CANCELLED);
        workspaceInvitationRepository.save(invitation);
        auditLogService.record(invitation.getWorkspace(), AuditAction.INVITATION_CANCELLED, user, invitation.getInvitedUser());
        log.debug("Invitación {} cancelada por {}", invitationId, user.getId());
    }

    @Transactional
    public void acceptRejectInvitation(@Valid AcceptRejectInvitationDTO invitationDTO) {
        var user = userService.getAuthenticatedUser();

        var invitation = workspaceInvitationRepository.findById(invitationDTO.id())
                .orElseThrow(() -> new EntityNotFoundException("Invitación inexistente"));

        if (!invitation.getInvitedUser().getId().equals(user.getId())) {
            log.warn("El usuario {} intentó responder una invitación que no le pertenece (id {})", user.getId(), invitationDTO.id());
            throw new EntityNotFoundException("Invitación inexistente");
        }
        if (!InvitationStatus.PENDING.equals(invitation.getStatus())) {
            throw new BusinessException("La invitación ya fue aceptada o rechazada");
        }
        invitation.setStatus(invitationDTO.status() ? InvitationStatus.ACCEPTED : InvitationStatus.REJECTED);
        workspaceInvitationRepository.save(invitation);
        auditLogService.record(invitation.getWorkspace(),
                invitationDTO.status() ? AuditAction.INVITATION_ACCEPTED : AuditAction.INVITATION_REJECTED,
                user, null);
        if (invitationDTO.status()) {
            workspaceMembershipService.addMembership(invitation.getWorkspace().getId(), user, invitation.getRole());
            invitationEventPublisher.publishInvitationAccepted(new InvitationAcceptedEvent(
                    invitation.getId(), invitation.getWorkspace().getId(), invitation.getWorkspace().getName(),
                    user.getEmail(), LocalDateTime.now(), CorrelationIdHolder.current()));
        }
        log.debug("Invitación {} actualizada correctamente a {}", invitationDTO.id(), invitation.getStatus());
    }
}