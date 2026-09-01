package com.api.identity.repositories;

import com.api.identity.entities.WorkspaceMember;
import com.api.identity.enums.WorkspaceRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {

    @Query("""
            select distinct m from WorkspaceMember m
            where m.workspace.isActive = true
              and m.workspace.id in (
                select wm.workspace.id from WorkspaceMember wm where wm.user.id = :userId
            )
            """)
    List<WorkspaceMember> findByWorkspaceOwnerOrMember(@Param("userId") Long userId);

    // join fetch de workspace + user: usado por el listado admin-wide de usuarios, que arma un
    // resumen para TODOS los usuarios de la instancia — sin esto, cada acceso a m.getWorkspace()/
    // m.getUser() en el loop dispararía una query lazy por fila (N+1).
    @Query("""
            select m from WorkspaceMember m
            join fetch m.workspace w
            join fetch m.user u
            where w.isActive = true
            """)
    List<WorkspaceMember> findAllActiveWithWorkspaceAndUser();

    boolean existsByWorkspaceIdAndUserId(Long workspaceId, Long userId);

    Optional<WorkspaceMember> findByWorkspaceIdAndUserId(Long workspaceId, Long userId);

    List<WorkspaceMember> findByWorkspaceId(Long workspaceId);

    Optional<WorkspaceMember> findByWorkspaceIdAndRole(Long workspaceId, WorkspaceRole role);

    void deleteByWorkspaceId(Long workspaceId);
}
