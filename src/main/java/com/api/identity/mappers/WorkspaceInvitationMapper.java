package com.api.identity.mappers;

import com.api.identity.entities.WorkspaceInvitation;
import com.api.identity.records.workspaces.WorkspaceInvitationDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface WorkspaceInvitationMapper {

    @Mapping(target = "workspaceId", source = "workspace.id")
    @Mapping(target = "workspaceName", source = "workspace.name")
    @Mapping(target = "invitedByEmail", source = "invitedBy.email")
    WorkspaceInvitationDTO toDTO(WorkspaceInvitation workspaceInvitation);

    List<WorkspaceInvitationDTO> toDTO(List<WorkspaceInvitation> workspaceInvitations);
}
