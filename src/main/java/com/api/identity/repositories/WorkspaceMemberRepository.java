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
            where m.workspace.isActive = true
              and m.workspace.id in (
                select wm.workspace.id from WorkspaceMember wm where wm.user.id = :userId
            )
            """)
    List<WorkspaceMember> findByWorkspaceOwnerOrMember(@Param("userId") Long userId);

    boolean existsByWorkspaceIdAndUserId(Long workspaceId, Long userId);

    Optional<WorkspaceMember> findByWorkspaceIdAndUserId(Long workspaceId, Long userId);

    List<WorkspaceMember> findByWorkspaceId(Long workspaceId);

    void deleteByWorkspaceId(Long workspaceId);
}
