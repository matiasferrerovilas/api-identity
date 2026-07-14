package com.api.identity.mappers;

import com.api.identity.entities.User;
import com.api.identity.records.UserMe;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    @Mapping(target = "metadata", expression = "java(toMetadata(isFirstLogin, hasSeenTour, roles))")
    UserMe toUserMe(User user, boolean isFirstLogin, boolean hasSeenTour, List<String> roles);

    default UserMe.Metadata toMetadata(boolean isFirstLogin, boolean hasSeenTour, List<String> roles) {
        return UserMe.Metadata.builder()
                .isFirstLogin(isFirstLogin)
                .hasSeenTour(hasSeenTour)
                .userRole(roles)
                .build();
    }
}
