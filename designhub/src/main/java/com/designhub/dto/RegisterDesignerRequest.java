package com.designhub.dto;

import jakarta.validation.constraints.Size;

public record RegisterDesignerRequest(
        @Size(max = 1000, message = "Bio không được vượt quá 1000 ký tự")
        String bio,

        @Size(max = 255, message = "Avatar không được vượt quá 255 ký tự")
        String avatar
) {
}
