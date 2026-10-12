package com.designhub.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.designhub.entity.User;
import com.designhub.repository.UserRepository;

@Service
@Transactional
public class UserService {

    private static final Set<String> VALID_ROLES = Set.of("ADMIN", "DESIGNER", "CUSTOMER");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public User createUser(User user) {
        String username = user.getUsername().trim();
        String email = user.getEmail().trim();

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username đã tồn tại");
        }

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email đã tồn tại");
        }

        user.setId(null);
        user.setUsername(username);
        user.setEmail(email);
        user.setRole(normalizeRole(user.getRole()));
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepository.save(user);
    }

    public Optional<User> updateUser(Long id, User user) {
        return userRepository.findById(id)
                .map(existingUser -> {
                    String username = user.getUsername().trim();
                    String email = user.getEmail().trim();

                    if (userRepository.existsByUsernameAndIdNot(username, id)) {
                        throw new IllegalArgumentException("Username đã tồn tại");
                    }

                    if (userRepository.existsByEmailAndIdNot(email, id)) {
                        throw new IllegalArgumentException("Email đã tồn tại");
                    }

                    existingUser.setUsername(username);
                    existingUser.setEmail(email);
                    existingUser.setFullName(user.getFullName());
                    existingUser.setPhone(user.getPhone());
                    existingUser.setRole(normalizeRole(user.getRole()));
                    existingUser.setPassword(
                            passwordEncoder.encode(user.getPassword()));

                    return userRepository.save(existingUser);
                });
    }

    public boolean deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            return false;
        }

        userRepository.deleteById(id);
        return true;
    }

    private String normalizeRole(String role) {
        String normalized = role.trim().toUpperCase();

        if (!VALID_ROLES.contains(normalized)) {
            throw new IllegalArgumentException(
                    "Role không hợp lệ. Chỉ chấp nhận: " + VALID_ROLES);
        }

        return normalized;
    }

}
