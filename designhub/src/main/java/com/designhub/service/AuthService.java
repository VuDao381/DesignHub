package com.designhub.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.designhub.dto.AuthResponse;
import com.designhub.dto.LoginRequest;
import com.designhub.dto.RegisterRequest;
import com.designhub.entity.User;
import com.designhub.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.username().trim();
        String email = request.email().trim();

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException(
                    "Username đã tồn tại");
        }

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "Email đã được sử dụng");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(
                passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setPhone(
                request.phone() == null || request.phone().isBlank()
                ? null
                : request.phone().trim());

        // Người đăng ký công khai mặc định là CUSTOMER.
        // Không nhận role từ request để tránh tự cấp quyền ADMIN.
        user.setRole("CUSTOMER");

        User savedUser = userRepository.save(user);

        return toAuthResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository
                .findByUsername(request.username().trim())
                .orElseThrow(()
                        -> new IllegalArgumentException(
                                "Username hoặc mật khẩu không đúng"));

        if (!passwordEncoder.matches(
                request.password(), user.getPassword())) {
            throw new IllegalArgumentException(
                    "Username hoặc mật khẩu không đúng");
        }

        return toAuthResponse(user);
    }

    private AuthResponse toAuthResponse(User user) {
        return new AuthResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getRole());
    }
}
