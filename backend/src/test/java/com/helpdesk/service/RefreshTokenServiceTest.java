package com.helpdesk.service;

import com.helpdesk.entity.RefreshToken;
import com.helpdesk.entity.Role;
import com.helpdesk.entity.User;
import com.helpdesk.exception.InvalidTokenException;
import com.helpdesk.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RefreshTokenServiceTest {
    @Mock RefreshTokenRepository repo;
    @InjectMocks RefreshTokenService service;

    User user;
    String raw;
    RefreshToken stored;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "days", 7L);
        user = User.builder().id(5L).name("Sam").email("sam@x.com").password("h").role(Role.EMPLOYEE).build();
        raw = service.issue(user);
        var captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repo).save(captor.capture());
        stored = captor.getValue();
        when(repo.findByTokenHash(anyString())).thenReturn(Optional.of(stored));
    }

    @Test
    void onlyAHashOfTheTokenIsStored() {
        assertNotEquals(raw, stored.getTokenHash());
        assertEquals(64, stored.getTokenHash().length());
        assertTrue(stored.getExpiresAt().isAfter(LocalDateTime.now().plusDays(6)));
    }

    @Test
    void rotationRevokesTheOldTokenAndIssuesANewOne() {
        var rotation = service.rotate(raw);
        assertTrue(stored.isRevoked());
        assertNotEquals(raw, rotation.refreshToken());
        assertEquals("sam@x.com", rotation.email());
    }

    @Test
    void reusingAUsedTokenRevokesEverySessionOfThatUser() {
        stored.setRevoked(true);
        assertThrows(InvalidTokenException.class, () -> service.rotate(raw));
        verify(repo).revokeAllForUser(5L);
    }

    @Test
    void expiredTokenIsRejected() {
        stored.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        assertThrows(InvalidTokenException.class, () -> service.rotate(raw));
    }

    @Test
    void unknownTokenIsRejected() {
        when(repo.findByTokenHash(anyString())).thenReturn(Optional.empty());
        assertThrows(InvalidTokenException.class, () -> service.rotate("nope"));
    }

    @Test
    void deactivatedUsersCannotRefresh() {
        user.setActive(false);
        assertThrows(InvalidTokenException.class, () -> service.rotate(raw));
    }
}
