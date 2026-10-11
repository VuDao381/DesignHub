package com.designhub.dto;

public record AuthResponse(
        Long id,
        String username,
        String email,
        String fullName,
        String phone,
        String role
        ) {}
