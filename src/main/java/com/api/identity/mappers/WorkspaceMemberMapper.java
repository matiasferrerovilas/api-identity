package com.api.identity.mappers;

import com.api.identity.entities.WorkspaceMember;
import com.api.identity.records.workspaces.WorkspaceMemberDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface WorkspaceMemberMapper {

    @Mapping(target = "workspaceId", source = "workspace.id")
    @Mapping(target = "workspaceName", source = "workspace.name")
    @Mapping(target = "metadata", source = ".")
    WorkspaceMemberDTO toDTO(WorkspaceMember workspaceMember);

    List<WorkspaceMemberDTO> toDTO(List<WorkspaceMember> workspaceMembers);

    default WorkspaceMemberDTO.Metadata toMetadata(WorkspaceMember workspaceMember) {
        return new WorkspaceMemberDTO.Metadata(
                List.of(workspaceMember.getUser().getEmail()),
                workspaceMember.getRole(),
                workspaceMember.getJoinedAt());
    }
}
