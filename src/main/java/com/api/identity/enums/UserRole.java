package com.api.identity.enums;

public enum UserRole {
    ROLE_ADMIN,
    ROLE_FAMILY,
    ROLE_GUEST;

    public static UserRole parse(String role) {
        try {
            return UserRole.valueOf(role);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
