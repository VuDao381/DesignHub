package com.designhub.dto;

import com.designhub.entity.Role;

public record AuthResponse(
        Long id,
        String username,
        String email,
        String fullName,
        String phone,
        Role role,
        String token,
        String refreshToken
) {
    public AuthResponse(Long id, String username, String email, String fullName, String phone, Role role, String token) {
        this(id, username, email, fullName, phone, role, token, null);
    }
}
