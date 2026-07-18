package com.api.identity.repositories;

import com.api.identity.entities.WorkspaceInvitation;
import com.api.identity.enums.InvitationStatus;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceInvitationRepository extends JpaRepository<WorkspaceInvitation, Long> {

    List<WorkspaceInvitation> findByInvitedUser_IdAndStatus(Long invitedUserId, InvitationStatus status);

    Optional<WorkspaceInvitation> findByWorkspace_IdAndStatusAndInvitedUser_Id(Long workspaceId, InvitationStatus status, Long invitedUserId);

}
