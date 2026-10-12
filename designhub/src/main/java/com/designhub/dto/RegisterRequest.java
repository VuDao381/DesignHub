package com.designhub.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
                @NotBlank(message = "Username không được để trống") @Size(min = 3, max = 50, message = "Username phải từ 3 đến 50 ký tự") String username,
                @NotBlank(message = "Email không được để trống") @Email(message = "Email không hợp lệ") @Size(max = 100, message = "Email không được vượt quá 100 ký tự") String email,
                @NotBlank(message = "Mật khẩu không được để trống") @Size(min = 8, max = 20, message = "Mật khẩu phải từ 8 đến 20 ký tự") String password,
                @NotBlank(message = "Họ tên không được để trống") @Size(max = 100, message = "Họ tên không được vượt quá 100 ký tự") String fullName,
                @Size(max = 20, message = "Số điện thoại không được vượt quá 20 ký tự") String phone) {
}
