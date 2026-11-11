package com.helpdesk.service;

import com.helpdesk.entity.RefreshToken;
import com.helpdesk.entity.Role;
import com.helpdesk.entity.User;
import com.helpdesk.exception.InvalidTokenException;
import com.helpdesk.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository repo;
    @Value("${app.jwt.refresh-expiration-days:7}")
    private long days;
    private final SecureRandom random = new SecureRandom();

    public record Rotation(Long userId, String name, String email, Role role, int tokenVersion, String refreshToken) {}

    @Transactional
    public String issue(User user) {
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        repo.save(RefreshToken.builder().tokenHash(hash(raw)).user(user)
                .expiresAt(LocalDateTime.now().plusDays(days)).build());
        return raw;
    }

    /**
     * Single-use rotation: the old token is revoked and a new one issued.
     * If an already-used token shows up again, assume theft and revoke every session of that user.
     */
    @Transactional(noRollbackFor = InvalidTokenException.class)
    public Rotation rotate(String raw) {
        var t = repo.findByTokenHash(hash(raw)).orElseThrow(() -> new InvalidTokenException("Invalid session. Sign in again."));
        var u = t.getUser();
        if (t.isRevoked()) {
            repo.revokeAllForUser(u.getId());
            throw new InvalidTokenException("This session was already used. Sign in again.");
        }
        if (t.getExpiresAt().isBefore(LocalDateTime.now()) || !u.isActive())
            throw new InvalidTokenException("Session expired. Sign in again.");
        t.setRevoked(true);
        return new Rotation(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.getTokenVersion(), issue(u));
    }

    @Transactional
    public void revoke(String raw) {
        repo.findByTokenHash(hash(raw)).ifPresent(t -> t.setRevoked(true));
    }

    @Transactional
    public void revokeAll(Long userId) { repo.revokeAllForUser(userId); }

    @Transactional
    public void cleanup() { repo.deleteByExpiresAtBefore(LocalDateTime.now().minusDays(1)); }

    private static String hash(String raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
