package com.helpdesk.service;

import com.helpdesk.dto.AuthDtos.LoginRequest;
import com.helpdesk.dto.AuthDtos.RegisterRequest;
import com.helpdesk.entity.Role;
import com.helpdesk.entity.User;
import com.helpdesk.exception.BadRequestException;
import com.helpdesk.exception.TooManyAttemptsException;
import com.helpdesk.repository.UserRepository;
import com.helpdesk.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {
    @Mock UserRepository users;
    @Mock PasswordEncoder encoder;
    @Mock AuthenticationManager authManager;
    @Mock JwtService jwt;
    @Mock RefreshTokenService refreshTokens;
    @InjectMocks AuthService service;

    User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "maxAttempts", 3);
        ReflectionTestUtils.setField(service, "lockMinutes", 15);
        user = User.builder().id(1L).name("Sam").email("sam@x.com").password("hash").role(Role.EMPLOYEE).build();
        when(users.findByEmail("sam@x.com")).thenReturn(Optional.of(user));
        when(jwt.generateToken(anyString(), anyString(), anyInt())).thenReturn("access");
        when(refreshTokens.issue(any())).thenReturn("refresh");
    }

    private LoginRequest login() { return new LoginRequest("sam@x.com", "whatever1"); }

    @Test
    void wrongPasswordCountsAsAFailedAttempt() {
        when(authManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));
        assertThrows(BadCredentialsException.class, () -> service.login(login()));
        assertEquals(1, user.getFailedAttempts());
        verify(users).save(user);
    }

    @Test
    void accountLocksAfterTooManyFailures() {
        when(authManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));
        for (int i = 0; i < 3; i++) assertThrows(BadCredentialsException.class, () -> service.login(login()));
        assertNotNull(user.getLockedUntil());
        assertTrue(user.getLockedUntil().isAfter(LocalDateTime.now()));
        assertThrows(TooManyAttemptsException.class, () -> service.login(login()));
    }

    @Test
    void correctPasswordIsStillRejectedWhileLocked() {
        user.setLockedUntil(LocalDateTime.now().plusMinutes(10));
        assertThrows(TooManyAttemptsException.class, () -> service.login(login()));
        verify(authManager, never()).authenticate(any());
    }

    @Test
    void successfulLoginResetsTheFailureCounter() {
        user.setFailedAttempts(2);
        var res = service.login(login());
        assertEquals("access", res.token());
        assertEquals("refresh", res.refreshToken());
        assertEquals(0, user.getFailedAttempts());
    }

    @Test
    void expiredLockNoLongerBlocksLogin() {
        user.setLockedUntil(LocalDateTime.now().minusMinutes(1));
        assertEquals("access", service.login(login()).token());
        assertNull(user.getLockedUntil());
    }

    @Test
    void registrationAlwaysCreatesAnEmployee() {
        when(users.existsByEmail("eve@x.com")).thenReturn(false);
        when(encoder.encode("passw0rd1")).thenReturn("hashed");
        when(users.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        service.register(new RegisterRequest("Eve", "eve@x.com", "passw0rd1"));
        var captor = ArgumentCaptor.forClass(User.class);
        verify(users).save(captor.capture());
        assertEquals(Role.EMPLOYEE, captor.getValue().getRole());
        assertEquals("hashed", captor.getValue().getPassword());
    }

    @Test
    void registeringAnExistingEmailFails() {
        when(users.existsByEmail("sam@x.com")).thenReturn(true);
        assertThrows(BadRequestException.class,
                () -> service.register(new RegisterRequest("Sam", "sam@x.com", "passw0rd1")));
    }

    @Test
    void changingPasswordRequiresTheCurrentOne() {
        when(encoder.matches("wrong", "hash")).thenReturn(false);
        assertThrows(BadRequestException.class, () -> service.changePassword("sam@x.com", "wrong", "newpass12"));
    }

    @Test
    void changingPasswordEndsEverySession() {
        when(encoder.matches("old", "hash")).thenReturn(true);
        when(encoder.encode("newpass12")).thenReturn("newhash");
        service.changePassword("sam@x.com", "old", "newpass12");
        assertEquals("newhash", user.getPassword());
        assertEquals(1, user.getTokenVersion());
        verify(refreshTokens).revokeAll(1L);
    }
}
