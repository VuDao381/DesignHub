package com.designhub.service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.designhub.dto.ChangePasswordRequest;
import com.designhub.dto.UpdateProfileRequest;
import com.designhub.dto.UserProfileResponse;
import com.designhub.entity.Role;
import com.designhub.entity.User;
import com.designhub.repository.UserRepository;
import com.designhub.security.CurrentUserService;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            CurrentUserService currentUserService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile() {
        User me = currentUserService.getCurrentUser();
        return UserProfileResponse.fromEntity(me);
    }

    public UserProfileResponse updateMyProfile(UpdateProfileRequest request) {
        User me = currentUserService.getCurrentUser();
        me.setFullName(request.fullName().trim());
        me.setPhone(request.phone() != null && !request.phone().isBlank() ? request.phone().trim() : null);
        User saved = userRepository.save(me);
        return UserProfileResponse.fromEntity(saved);
    }

    public void changePassword(ChangePasswordRequest request) {
        User me = currentUserService.getCurrentUser();
        if (!passwordEncoder.matches(request.currentPassword(), me.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không chính xác");
        }
        if (passwordEncoder.matches(request.newPassword(), me.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu mới không được trùng với mật khẩu hiện tại");
        }
        me.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(me);
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

        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password không được để trống");
        }
        if (user.getPassword().length() < 8) {
            throw new IllegalArgumentException("Password phải có ít nhất 8 ký tự");
        }

        user.setId(null);
        user.setUsername(username);
        user.setEmail(email);
        user.setRole(user.getRole() != null ? user.getRole() : Role.CUSTOMER);
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
                    if (user.getRole() != null) {
                        existingUser.setRole(user.getRole());
                    }

                    if (user.getPassword() != null && !user.getPassword().isBlank()) {
                        if (user.getPassword().length() < 8) {
                            throw new IllegalArgumentException("Password phải có ít nhất 8 ký tự");
                        }
                        existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
                    }

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
}
