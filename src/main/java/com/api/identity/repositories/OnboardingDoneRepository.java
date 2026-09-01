package com.api.identity.repositories;

import com.api.identity.entities.OnboardingDone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OnboardingDoneRepository extends JpaRepository<OnboardingDone, Long> {
    Optional<OnboardingDone> findByUserEmailAndApiName(String email, String api);

    // join fetch de user + api: usado por el listado admin-wide de usuarios, mismo motivo que
    // WorkspaceMemberRepository.findAllActiveWithWorkspaceAndUser (evitar N+1 al recorrer todos
    // los usuarios de la instancia).
    @Query("""
            select o from OnboardingDone o
            join fetch o.user u
            join fetch o.api a
            """)
    List<OnboardingDone> findAllWithUserAndApi();

    @Modifying
    @Query(value = """
     UPDATE OnboardingDone o
        SET o.hasSeenTour = true
      WHERE o.user.email = :email
        AND o.api.name = :api
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
