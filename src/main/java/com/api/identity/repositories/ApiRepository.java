package com.api.identity.repositories;

import com.api.identity.entities.Api;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApiRepository extends JpaRepository<Api, Long> {
    Optional<Api> findByName(String name);
}
