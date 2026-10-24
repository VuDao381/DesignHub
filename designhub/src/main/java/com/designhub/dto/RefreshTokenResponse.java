package com.designhub.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RefreshTokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType
) {
    public RefreshTokenResponse(String accessToken, String refreshToken) {
        this(accessToken, refreshToken, "Bearer");
    }

    @JsonProperty("token")
    public String token() {
        return accessToken;
    }
}
