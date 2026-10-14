package com.designhub.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.designhub.dto.AuthResponse;
import com.designhub.dto.LoginRequest;
import com.designhub.dto.RegisterRequest;
import com.designhub.entity.Role;
import com.designhub.entity.User;
import com.designhub.repository.UserRepository;
import com.designhub.security.JwtService;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
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

        user.setRole(Role.CUSTOMER);

        User savedUser = userRepository.save(user);

        return toAuthResponse(savedUser, null);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository
                .findByUsername(request.username().trim())
                .orElseThrow(()
                        -> new org.springframework.security.authentication.BadCredentialsException(
                                "Username hoặc mật khẩu không đúng"));

        if (!passwordEncoder.matches(
                request.password(), user.getPassword())) {
            throw new org.springframework.security.authentication.BadCredentialsException(
                    "Username hoặc mật khẩu không đúng");
        }

        String token = jwtService.generateToken(
                user.getUsername(),
                user.getRole().name());

        return toAuthResponse(user, token);
    }

    private AuthResponse toAuthResponse(User user, String token) {
        return new AuthResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getRole(),
                token);
    }
}
