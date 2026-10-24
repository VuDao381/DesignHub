package com.designhub.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
                @NotBlank(message = "Email hoặc Tên đăng nhập không được để trống") String email,
                @NotBlank(message = "Mật khẩu không được để trống") String password) {
}
