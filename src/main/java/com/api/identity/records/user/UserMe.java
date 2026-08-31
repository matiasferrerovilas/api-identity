package com.api.identity.records.user;

import com.api.identity.enums.WorkspaceRole;
import lombok.Builder;

import java.util.List;

@Builder(toBuilder = true)
public record UserMe(
        Long id,
        String email,
        String givenName,
        String familyName,
        String userType,
        Metadata metadata
) {
    @Builder(toBuilder = true)
    public record Metadata(
            boolean isFirstLogin,
            boolean hasSeenTour,
            List<String> userRole,
            /** The caller's role in the workspace passed as {@code ?workspaceId=} — null if no
             * workspaceId was given, or if the caller isn't a member of that workspace. Distinct
             * from {@code userRole}, which is the global suite-wide role (ADMIN/FAMILY/GUEST). */
            WorkspaceRole workspaceRole
    ) { }
}
