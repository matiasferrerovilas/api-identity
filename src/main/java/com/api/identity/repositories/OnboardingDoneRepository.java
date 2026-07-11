package com.api.identity.repositories;

import com.api.identity.entities.OnboardingDone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OnboardingDoneRepository extends JpaRepository<OnboardingDone, Long> {
    @Modifying
    @Query(value = """
     UPDATE OnboardingDone o
        SET o.hasSeenTour = true
      WHERE o.user.email = :email
        AND o.api = :api
""")
    int markTourAsSeen(String email, String api);
}
