package com.designhub.service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.designhub.dto.ChangePasswordRequest;
import com.designhub.dto.UpdateProfileRequest;
import com.designhub.dto.UserProfileResponse;
import com.designhub.entity.Designer;
import com.designhub.entity.Role;
import com.designhub.entity.User;
import com.designhub.repository.DesignRepository;
import com.designhub.repository.DesignerRepository;
import com.designhub.repository.OrderRepository;
import com.designhub.repository.UserRepository;
import com.designhub.security.CurrentUserService;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final DesignerRepository designerRepository;
    private final DesignRepository designRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;
    private final RefreshTokenService refreshTokenService;

    public UserService(
            UserRepository userRepository,
            DesignerRepository designerRepository,
            DesignRepository designRepository,
            OrderRepository orderRepository,
            PasswordEncoder passwordEncoder,
            CurrentUserService currentUserService) {
        this(userRepository, designerRepository, designRepository, orderRepository, passwordEncoder, currentUserService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public UserService(
            UserRepository userRepository,
            DesignerRepository designerRepository,
            DesignRepository designRepository,
            OrderRepository orderRepository,
            PasswordEncoder passwordEncoder,
            CurrentUserService currentUserService,
            RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.designerRepository = designerRepository;
        this.designRepository = designRepository;
        this.orderRepository = orderRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUserService = currentUserService;
        this.refreshTokenService = refreshTokenService;
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
        if (request.newPassword().length() < 8 || request.newPassword().length() > 72) {
            throw new IllegalArgumentException("Mật khẩu mới phải từ 8 đến 72 ký tự");
        }
        me.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(me);
        if (refreshTokenService != null) {
            refreshTokenService.deleteByUser(me);
        }
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
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
        if (user.getPassword().length() < 8 || user.getPassword().length() > 72) {
            throw new IllegalArgumentException("Password phải từ 8 đến 72 ký tự");
        }

        user.setId(null);
        user.setUsername(username);
        user.setEmail(email);
        user.setRole(user.getRole() != null ? user.getRole() : Role.CUSTOMER);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        User savedUser = userRepository.save(user);
        if (savedUser.getRole() == Role.DESIGNER && designerRepository != null
                && designerRepository.findByUserId(savedUser.getId()).isEmpty()) {
            Designer designer = new Designer();
            designer.setUser(savedUser);
            designerRepository.save(designer);
        }
        return savedUser;
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
                        if (existingUser.getRole() == Role.ADMIN && user.getRole() != Role.ADMIN) {
                            if (userRepository.countByRole(Role.ADMIN) <= 1) {
                                throw new IllegalArgumentException("Không thể hạ quyền Quản trị viên (ADMIN) duy nhất còn lại trong hệ thống");
                            }
                        }
                        existingUser.setRole(user.getRole());
                    }

                    if (user.getPassword() != null && !user.getPassword().isBlank()) {
                        if (user.getPassword().length() < 8 || user.getPassword().length() > 72) {
                            throw new IllegalArgumentException("Password phải từ 8 đến 72 ký tự");
                        }
                        existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
                        if (refreshTokenService != null) {
                            refreshTokenService.deleteByUser(existingUser);
                        }
                    }

                    User savedUser = userRepository.save(existingUser);
                    if (savedUser.getRole() == Role.DESIGNER && designerRepository != null
                            && designerRepository.findByUserId(savedUser.getId()).isEmpty()) {
                        Designer designer = new Designer();
                        designer.setUser(savedUser);
                        designerRepository.save(designer);
                    }
                    return savedUser;
                });
    }

    public boolean deleteUser(Long id) {
        User userToDelete = userRepository.findById(id).orElse(null);
        if (userToDelete == null) {
            return false;
        }

        try {
            User me = currentUserService.getCurrentUser();
            if (me.getId().equals(id)) {
                throw new IllegalArgumentException("Bạn không thể tự xóa tài khoản của chính mình");
            }
        } catch (org.springframework.security.access.AccessDeniedException ignored) {
        }

        if (userToDelete.getRole() == Role.ADMIN && userRepository.countByRole(Role.ADMIN) <= 1) {
            throw new IllegalArgumentException("Không thể xóa tài khoản Quản trị viên (ADMIN) duy nhất còn lại trong hệ thống");
        }

        if (orderRepository.existsByUserId(id)) {
            throw new IllegalStateException("Không thể xóa người dùng đã có lịch sử đơn hàng trong hệ thống.");
        }

        Optional<Designer> designerOpt = designerRepository.findByUserId(id);
        if (designerOpt.isPresent()) {
            if (designRepository.existsByDesignerId(designerOpt.get().getId())) {
                throw new IllegalStateException("Không thể xóa người dùng là Designer đang có các thiết kế trong hệ thống.");
            }
            designerRepository.delete(designerOpt.get());
        }

        if (refreshTokenService != null) {
            refreshTokenService.deleteByUser(userToDelete);
        }

        userRepository.delete(userToDelete);
        return true;
    }
}
