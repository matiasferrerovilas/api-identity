package com.api.identity.records.user;

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
    @Builder
    public record Metadata(
            boolean isFirstLogin,
            boolean hasSeenTour,
            List<String> userRole
    ) { }
}
