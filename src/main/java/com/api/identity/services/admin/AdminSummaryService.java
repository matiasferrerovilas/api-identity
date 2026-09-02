package com.api.identity.services.admin;

import com.api.identity.enums.InvitationStatus;
import com.api.identity.records.admin.AdminSummaryDTO;
import com.api.identity.repositories.UserRepository;
import com.api.identity.repositories.WorkspaceInvitationRepository;
import com.api.identity.repositories.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Números agregados para la portada del panel admin — protegido a nivel de
 * SecurityConfiguration ({@code /v1/admin/**} → ROLE_ADMIN), no acá, ver AdminController.
 */
@Service
@RequiredArgsConstructor
public class AdminSummaryService {

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceInvitationRepository workspaceInvitationRepository;

    @Transactional(readOnly = true)
    public AdminSummaryDTO getSummary() {
        var startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        return new AdminSummaryDTO(
                userRepository.count(),
                workspaceRepository.countByIsActiveTrue(),
                workspaceRepository.countByIsActiveTrueAndCreatedAtGreaterThanEqual(startOfMonth),
                workspaceInvitationRepository.countByStatus(InvitationStatus.PENDING));
    }
}
