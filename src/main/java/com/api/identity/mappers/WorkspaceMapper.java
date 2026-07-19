package com.api.identity.mappers;

import com.api.identity.entities.Workspace;
import com.api.identity.entities.WorkspaceMember;
import com.api.identity.enums.WorkspaceRole;
import com.api.identity.records.workspaces.WorkspaceDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.Set;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface WorkspaceMapper {

    @Mapping(target = "owner", expression = "java(resolveOwnerEmail(workspace))")
    @Mapping(target = "metadata", source = "members")
    WorkspaceDTO toDTO(Workspace workspace);

    default WorkspaceDTO.Metadata toMetadata(Set<WorkspaceMember> members) {
        return new WorkspaceDTO.Metadata(
                members.stream().map(m -> m.getUser().getEmail()).toList());
    }

    default String resolveOwnerEmail(Workspace workspace) {
        return workspace.getMembers().stream()
                .filter(m -> m.getRole() == WorkspaceRole.OWNER)
                .map(m -> m.getUser().getEmail())
                .findFirst()
                .orElse(null);
    }
}
