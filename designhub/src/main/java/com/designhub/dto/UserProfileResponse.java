package com.designhub.dto;

import java.time.LocalDateTime;

import com.designhub.entity.Role;
import com.designhub.entity.User;

public record UserProfileResponse(
        Long id,
        String username,
        String email,
        String fullName,
        String phone,
        Role role,
        LocalDateTime createdAt
) {
    public static UserProfileResponse fromEntity(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
