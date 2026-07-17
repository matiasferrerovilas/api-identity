package com.api.identity.repositories;

import com.api.identity.entities.OnboardingDone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OnboardingDoneRepository extends JpaRepository<OnboardingDone, Long> {
    Optional<OnboardingDone> findByUser_EmailAndApi(String email, String api);

    @Modifying
    @Query(value = """
     UPDATE OnboardingDone o
        SET o.hasSeenTour = true
      WHERE o.user.email = :email
        AND o.api = :api
""")
    int markTourAsSeen(String email, String api);

    @Modifying
    @Query(value = """
     UPDATE OnboardingDone o
        SET o.isFirstLogin = false
      WHERE o.user.id = :userId
""")
    int markFirstLoginAsDone(Long userId);
}
