package com.api.identity.repositories;

import com.api.identity.entities.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

    List<Workspace> findByOwnerIdAndNameIn(Long ownerId, Collection<String> names);

    @Query("""
            select distinct w from Workspace w
            left join w.members m
            where w.owner.id = :userId or m.user.id = :userId
            """)
    List<Workspace> findByUserIn(@Param("userId") Long userId);
}
