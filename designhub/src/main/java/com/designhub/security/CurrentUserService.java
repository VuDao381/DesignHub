package com.designhub.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.designhub.entity.User;
import com.designhub.repository.UserRepository;

@Component
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder
                .getContext().getAuthentication();

        if (auth == null
                || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Chưa đăng nhập");
        }

        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new AccessDeniedException(
                        "Tài khoản không tồn tại"));
    }

    public boolean isAdmin() {
        Authentication auth = SecurityContextHolder
                .getContext().getAuthentication();

        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    public void checkOwnerOrAdmin(Long ownerId) {
        if (isAdmin()) {
            return;
        }

        if (!getCurrentUser().getId().equals(ownerId)) {
            throw new AccessDeniedException(
                    "Bạn không có quyền truy cập tài nguyên này");
        }
    }
}
