package com.helpdesk.service;

import com.helpdesk.dto.AuthDtos.AuthResponse;
import com.helpdesk.dto.AuthDtos.LoginRequest;
import com.helpdesk.dto.AuthDtos.RegisterRequest;
import com.helpdesk.entity.Role;
import com.helpdesk.entity.User;
import com.helpdesk.exception.BadRequestException;
import com.helpdesk.exception.NotFoundException;
import com.helpdesk.exception.TooManyAttemptsException;
import com.helpdesk.repository.UserRepository;
import com.helpdesk.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/** Deliberately not @Transactional: failed-login counters must be saved even though the login throws. */
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtService jwt;
    private final RefreshTokenService refreshTokens;

    @Value("${app.security.max-failed-attempts:5}")
    private int maxAttempts;
    @Value("${app.security.lock-minutes:15}")
    private int lockMinutes;

    public AuthResponse register(RegisterRequest r) {
        if (users.existsByEmail(r.email())) throw new BadRequestException("Email already registered");
        // Public registration always creates EMPLOYEE; admins promote users later.
        var u = users.save(User.builder().name(r.name().trim()).email(r.email().trim())
                .password(encoder.encode(r.password())).role(Role.EMPLOYEE).build());
        return issueFor(u);
    }

    public AuthResponse login(LoginRequest r) {
        var user = users.findByEmail(r.email()).orElse(null);
        if (user != null && user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            long mins = Duration.between(LocalDateTime.now(), user.getLockedUntil()).toMinutes() + 1;
            throw new TooManyAttemptsException("Too many failed attempts. Try again in " + mins + " minute(s).");
        }
        try {
            authManager.authenticate(new UsernamePasswordAuthenticationToken(r.email(), r.password()));
        } catch (BadCredentialsException e) {
            if (user != null) recordFailure(user);
            throw e;
        }
        if (user.getFailedAttempts() > 0 || user.getLockedUntil() != null) {
            user.setFailedAttempts(0);
            user.setLockedUntil(null);
            users.save(user);
        }
        return issueFor(user);
    }

    public AuthResponse refresh(String rawRefreshToken) {
        var rot = refreshTokens.rotate(rawRefreshToken);
        return new AuthResponse(jwt.generateToken(rot.email(), rot.role().name(), rot.tokenVersion()),
                rot.refreshToken(), rot.userId(), rot.name(), rot.email(), rot.role());
    }

    public void logout(String rawRefreshToken) { refreshTokens.revoke(rawRefreshToken); }

    public void changePassword(String email, String current, String next) {
        var u = users.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
        if (!encoder.matches(current, u.getPassword())) throw new BadRequestException("Current password is incorrect");
        if (encoder.matches(next, u.getPassword())) throw new BadRequestException("New password must be different from the current one");
        u.setPassword(encoder.encode(next));
        endAllSessions(u);
    }

    public void signOutEverywhere(String email) {
        endAllSessions(users.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found")));
    }

    // ---- helpers ----
    private void endAllSessions(User u) {
        u.setTokenVersion(u.getTokenVersion() + 1);   // kills every access token issued so far
        users.save(u);
        refreshTokens.revokeAll(u.getId());           // kills every refresh token
    }

    private void recordFailure(User u) {
        int n = u.getFailedAttempts() + 1;
        if (n >= maxAttempts) {
            u.setLockedUntil(LocalDateTime.now().plusMinutes(lockMinutes));
            u.setFailedAttempts(0);
        } else {
            u.setFailedAttempts(n);
        }
        users.save(u);
    }

    private AuthResponse issueFor(User u) {
        return new AuthResponse(jwt.generateToken(u.getEmail(), u.getRole().name(), u.getTokenVersion()),
                refreshTokens.issue(u), u.getId(), u.getName(), u.getEmail(), u.getRole());
    }
}
