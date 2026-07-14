package com.api.identity.mappers;

import com.api.identity.entities.Workspace;
import com.api.identity.records.workspaces.WorkspaceDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface WorkspaceMapper {

    @Mapping(target = "owner", source = "owner.email")
    WorkspaceDTO toDTO(Workspace workspace);

    List<WorkspaceDTO> toDTO(List<Workspace> workspace);
}
