package com.api.identity.repositories;

import com.api.identity.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @Query(value = """
     SELECT u
        FROM User u
        WHERE u.email = :email
""")
    Optional<User> findByEmail(String email);

    @Query(value = """
     SELECT u
        FROM User u
        WHERE u.email in :email
""")
    List<User> findByEmail(List<String> email);
}