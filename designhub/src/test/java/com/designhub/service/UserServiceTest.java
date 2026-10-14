package com.designhub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.designhub.dto.ChangePasswordRequest;
import com.designhub.dto.UpdateProfileRequest;
import com.designhub.dto.UserProfileResponse;
import com.designhub.entity.Role;
import com.designhub.entity.User;
import com.designhub.repository.UserRepository;
import com.designhub.security.CurrentUserService;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CurrentUserService currentUserService;

    private UserService userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder, currentUserService);

        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setUsername("testuser");
        sampleUser.setEmail("test@designhub.com");
        sampleUser.setFullName("Nguyen Van A");
        sampleUser.setPassword("encodedOldPassword");
        sampleUser.setRole(Role.CUSTOMER);
    }

    @Test
    void testGetMyProfile() {
        when(currentUserService.getCurrentUser()).thenReturn(sampleUser);

        UserProfileResponse response = userService.getMyProfile();

        assertNotNull(response);
        assertEquals(sampleUser.getUsername(), response.username());
        assertEquals(sampleUser.getEmail(), response.email());
        assertEquals(sampleUser.getFullName(), response.fullName());
    }

    @Test
    void testUpdateMyProfile() {
        when(currentUserService.getCurrentUser()).thenReturn(sampleUser);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest("Tran Thi B", "0987654321");
        UserProfileResponse response = userService.updateMyProfile(request);

        assertEquals("Tran Thi B", response.fullName());
        assertEquals("0987654321", response.phone());
        verify(userRepository).save(sampleUser);
    }

    @Test
    void testChangePasswordSuccess() {
        when(currentUserService.getCurrentUser()).thenReturn(sampleUser);
        when(passwordEncoder.matches("OldPassword@123", "encodedOldPassword")).thenReturn(true);
        when(passwordEncoder.matches("NewPassword@123", "encodedOldPassword")).thenReturn(false);
        when(passwordEncoder.encode("NewPassword@123")).thenReturn("encodedNewPassword");

        ChangePasswordRequest request = new ChangePasswordRequest("OldPassword@123", "NewPassword@123");
        userService.changePassword(request);

        assertEquals("encodedNewPassword", sampleUser.getPassword());
        verify(userRepository).save(sampleUser);
    }

    @Test
    void testChangePasswordFailsWhenWrongCurrentPassword() {
        when(currentUserService.getCurrentUser()).thenReturn(sampleUser);
        when(passwordEncoder.matches("WrongPassword", "encodedOldPassword")).thenReturn(false);

        ChangePasswordRequest request = new ChangePasswordRequest("WrongPassword", "NewPassword@123");
        assertThrows(IllegalArgumentException.class, () -> userService.changePassword(request));
    }
}
