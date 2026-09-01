package com.api.identity.mappers;

import com.api.identity.entities.User;
import com.api.identity.entities.WorkspaceMember;
import com.api.identity.records.admin.AdminUserSummaryDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AdminUserMapper {

    // Set.copyOf fuerza la lectura de la colección lazy (userRoles es @ElementCollection LAZY)
    // acá adentro, mientras la transacción del caller todavía tiene sesión abierta — sin esto,
    // el Set queda como referencia al PersistentSet de Hibernate sin materializar, y Jackson
    // revienta con LazyInitializationException al serializar la respuesta (eso ocurre después
    // de que el método @Transactional ya devolvió y la sesión se cerró).
    @Mapping(target = "userRoles", expression = "java(java.util.Set.copyOf(user.getUserRoles()))")
    @Mapping(target = "workspaces", source = "workspaces")
    AdminUserSummaryDTO toAdminUserSummaryDTO(User user, List<AdminUserSummaryDTO.WorkspaceMembershipSummary> workspaces);

    @Mapping(target = "workspaceId", source = "workspace.id")
    @Mapping(target = "workspaceName", source = "workspace.name")
    AdminUserSummaryDTO.WorkspaceMembershipSummary toWorkspaceMembershipSummary(WorkspaceMember member);

    List<AdminUserSummaryDTO.WorkspaceMembershipSummary> toWorkspaceMembershipSummaries(List<WorkspaceMember> members);
}
