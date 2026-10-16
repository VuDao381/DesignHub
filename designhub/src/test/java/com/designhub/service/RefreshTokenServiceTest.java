package com.designhub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.designhub.entity.RefreshToken;
import com.designhub.entity.User;
import com.designhub.repository.RefreshTokenRepository;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService refreshTokenService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, 604800000L);

        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setEmail("test@designhub.com");
    }

    @Test
    void testCreateRefreshToken() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken token = refreshTokenService.createRefreshToken(sampleUser);

        assertNotNull(token);
        assertNotNull(token.getToken());
        assertEquals(sampleUser, token.getUser());
        verify(refreshTokenRepository).deleteByUser(sampleUser);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void testVerifyExpirationValidToken() {
        RefreshToken token = new RefreshToken();
        token.setExpiryDate(LocalDateTime.now().plusDays(2));
        token.setRevoked(false);

        RefreshToken verified = refreshTokenService.verifyExpiration(token);
        assertNotNull(verified);
    }

    @Test
    void testVerifyExpirationExpiredTokenThrows() {
        RefreshToken token = new RefreshToken();
        token.setExpiryDate(LocalDateTime.now().minusDays(1));
        token.setRevoked(false);

        assertThrows(IllegalArgumentException.class, () -> refreshTokenService.verifyExpiration(token));
        verify(refreshTokenRepository).delete(token);
    }

    @Test
    void testRevokeToken() {
        RefreshToken token = new RefreshToken();
        token.setToken("dummy-token");

        when(refreshTokenRepository.findByToken("dummy-token")).thenReturn(Optional.of(token));

        refreshTokenService.revokeToken("dummy-token");
        verify(refreshTokenRepository).delete(token);
    }
}
