package com.designhub.config;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.designhub.entity.Role;
import com.designhub.entity.User;
import com.designhub.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdminInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminInitializer adminInitializer;

    @BeforeEach
    void setUp() {
        adminInitializer = new AdminInitializer(userRepository, passwordEncoder);
    }

    @Test
    void testFailFastWhenMissingAdminPassword() {
        ReflectionTestUtils.setField(adminInitializer, "adminUsername", "admin");
        ReflectionTestUtils.setField(adminInitializer, "adminEmail", "admin@designhub.com");
        ReflectionTestUtils.setField(adminInitializer, "adminPassword", ""); // empty / missing

        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> adminInitializer.run());

        assertTrue(ex.getMessage().contains("Thiếu cấu hình tài khoản Quản trị viên khởi tạo"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testFailFastWhenMissingAdminEmail() {
        ReflectionTestUtils.setField(adminInitializer, "adminUsername", "admin");
        ReflectionTestUtils.setField(adminInitializer, "adminEmail", "   ");
        ReflectionTestUtils.setField(adminInitializer, "adminPassword", "SecretPass@123");

        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> adminInitializer.run());

        assertTrue(ex.getMessage().contains("Thiếu cấu hình tài khoản Quản trị viên khởi tạo"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testInitializesAdminSuccessfullyWhenConfigured() {
        ReflectionTestUtils.setField(adminInitializer, "adminUsername", "admin");
        ReflectionTestUtils.setField(adminInitializer, "adminEmail", "admin@designhub.com");
        ReflectionTestUtils.setField(adminInitializer, "adminPassword", "AdminPassword@123");

        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
        when(passwordEncoder.encode("AdminPassword@123")).thenReturn("encodedSecretPass");

        adminInitializer.run();

        verify(userRepository).save(any(User.class));
    }
}
