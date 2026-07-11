package com.api.identity.records;

import com.api.identity.enums.UserType;
import jakarta.validation.constraints.NotNull;

public record UserTypeUpdateRequest(
        @NotNull(message = "El tipo de usuario es obligatorio")
        UserType userType
) { }
