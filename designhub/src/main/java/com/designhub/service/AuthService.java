package com.designhub.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.designhub.dto.AuthResponse;
import com.designhub.dto.LoginRequest;
import com.designhub.dto.RefreshTokenRequest;
import com.designhub.dto.RefreshTokenResponse;
import com.designhub.dto.RegisterRequest;
import com.designhub.entity.RefreshToken;
import com.designhub.entity.Role;
import com.designhub.entity.User;
import com.designhub.repository.UserRepository;
import com.designhub.security.JwtService;

@Service
public class AuthService {

        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtService jwtService;
        private final RefreshTokenService refreshTokenService;

        public AuthService(
                        UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService,
                        RefreshTokenService refreshTokenService) {
                this.userRepository = userRepository;
                this.passwordEncoder = passwordEncoder;
                this.jwtService = jwtService;
                this.refreshTokenService = refreshTokenService;
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

                String token = jwtService.generateToken(
                                savedUser.getEmail(),
                                savedUser.getRole().name());

                RefreshToken refreshToken = refreshTokenService.createRefreshToken(savedUser);

                return toAuthResponse(savedUser, token, refreshToken.getToken());
        }

        @Transactional
        public AuthResponse login(LoginRequest request) {
                String identifier = request.email().trim();
                User user = userRepository
                                .findByEmail(identifier)
                                .or(() -> userRepository.findByUsername(identifier))
                                .orElseThrow(() -> new org.springframework.security.authentication.BadCredentialsException(
                                                "Email/Tài khoản hoặc mật khẩu không đúng"));

                if (!passwordEncoder.matches(
                                request.password(), user.getPassword())) {
                        throw new org.springframework.security.authentication.BadCredentialsException(
                                        "Email/Tài khoản hoặc mật khẩu không đúng");
                }

                String token = jwtService.generateToken(
                                user.getEmail(),
                                user.getRole().name());

                RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

                return toAuthResponse(user, token, refreshToken.getToken());
        }

        @Transactional
        public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
                RefreshToken refreshToken = refreshTokenService.findByToken(request.refreshToken())
                                .map(refreshTokenService::verifyExpiration)
                                .orElseThrow(() -> new IllegalArgumentException("Refresh token không hợp lệ hoặc đã hết hạn!"));

                User user = refreshToken.getUser();
                String newAccessToken = jwtService.generateToken(user.getEmail(), user.getRole().name());

                return new RefreshTokenResponse(newAccessToken, refreshToken.getToken());
        }

        @Transactional
        public void logout(RefreshTokenRequest request) {
                refreshTokenService.revokeToken(request.refreshToken());
        }

        private AuthResponse toAuthResponse(User user, String token, String refreshToken) {
                return new AuthResponse(
                                user.getId(),
                                user.getUsername(),
                                user.getEmail(),
                                user.getFullName(),
                                user.getPhone(),
                                user.getRole(),
                                token,
                                refreshToken);
        }
}
