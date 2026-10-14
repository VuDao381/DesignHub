package com.designhub.dto;

import com.designhub.entity.Role;

public record AuthResponse(
        Long id,
        String username,
        String email,
        String fullName,
        String phone,
        Role role,
        String token
) {}
