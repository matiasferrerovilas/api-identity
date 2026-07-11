package com.api.identity.records;

public record UserMe(
        Long id,
        String email,
        String givenName,
        String familyName,
        String userType,
        Metadata metadata
) {
    public record Metadata(
            boolean isFirstLogin,
            boolean hasSeenTour,
            String userRole
    ) { }
}
