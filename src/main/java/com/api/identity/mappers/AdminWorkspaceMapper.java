package com.api.identity.mappers;

import com.api.identity.entities.Workspace;
import com.api.identity.entities.WorkspaceMember;
import com.api.identity.records.admin.AdminWorkspaceSummaryDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AdminWorkspaceMapper {

    @Mapping(target = "members", source = "members")
    AdminWorkspaceSummaryDTO toAdminWorkspaceSummaryDTO(Workspace workspace, List<AdminWorkspaceSummaryDTO.MemberSummary> members);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "givenName", source = "user.givenName")
    @Mapping(target = "familyName", source = "user.familyName")
    AdminWorkspaceSummaryDTO.MemberSummary toMemberSummary(WorkspaceMember member);

    List<AdminWorkspaceSummaryDTO.MemberSummary> toMemberSummaries(List<WorkspaceMember> members);
}
