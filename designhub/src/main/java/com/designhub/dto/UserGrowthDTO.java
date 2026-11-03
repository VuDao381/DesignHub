package com.designhub.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserGrowthDTO(
        int year,
        int month,
        long count
) {
    @JsonProperty("userCount")
    public long userCount() {
        return count;
    }
}
