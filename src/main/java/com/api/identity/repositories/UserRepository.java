package com.api.identity.repositories;

import com.api.identity.entities.User;
import com.api.identity.enums.UserType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    @Modifying
    @Query(value = """
     UPDATE User u
        SET u.userType = :userType,
            u.updatedAt = CURRENT_TIMESTAMP
      WHERE u.email = :email
""")
    int updateUserType(String email, UserType userType);
}