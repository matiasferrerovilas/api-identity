package com.api.identity.repositories;

import com.api.identity.entities.WorkspaceMember;
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
            where m.workspace.owner.id = :userId
               or m.workspace.id in (
                   select wm.workspace.id from WorkspaceMember wm where wm.user.id = :userId
               )
            """)
    List<WorkspaceMember> findByWorkspaceOwnerOrMember(@Param("userId") Long userId);

    boolean existsByWorkspace_IdAndUser_Id(Long workspaceId, Long userId);

    Optional<WorkspaceMember> findByWorkspace_IdAndUser_Id(Long workspaceId, Long userId);

    List<WorkspaceMember> findByWorkspace_Id(Long workspaceId);

    void deleteByWorkspace_Id(Long workspaceId);
}
