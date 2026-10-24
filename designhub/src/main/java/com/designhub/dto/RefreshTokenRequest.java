package com.designhub.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @JsonAlias("token")
        @NotBlank(message = "Refresh token không được để trống")
        String refreshToken
) {}
