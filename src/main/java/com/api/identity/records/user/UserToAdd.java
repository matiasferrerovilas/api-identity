package com.api.identity.records.user;

import com.api.identity.enums.UserType;
import lombok.Builder;

@Builder
public record UserToAdd(String email, String givenName, String familyName, boolean isFirstLogin, UserType userType) {
}
