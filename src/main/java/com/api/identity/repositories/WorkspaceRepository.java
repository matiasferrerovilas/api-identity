package com.api.identity.repositories;

import com.api.identity.entities.Workspace;
import com.api.identity.enums.WorkspaceRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

    @Query("""
            select distinct w from Workspace w
            join w.members m
            where m.user.id = :ownerId and m.role = :role and w.name in :names
            """)
    List<Workspace> findByOwnerIdAndNameIn(
            @Param("ownerId") Long ownerId,
            @Param("names") Collection<String> names,
            @Param("role") WorkspaceRole role);

    @Query("""
            select distinct w from Workspace w
            join w.members m
            where m.user.id = :userId
            """)
    List<Workspace> findByUserIn(@Param("userId") Long userId);
}
