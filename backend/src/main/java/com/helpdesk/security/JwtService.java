package com.helpdesk.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/** Short-lived access tokens. Refresh tokens are separate, opaque and stored hashed in the DB. */
@Slf4j
@Service
public class JwtService {
    private static final String ISSUER = "helpdesk";
    private final SecretKey key;
    private final long accessMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.access-expiration-ms}") long accessMs) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32)
            throw new IllegalStateException("app.jwt.secret must be at least 32 characters long");
        if (secret.startsWith("change-this"))
            log.warn("Using the demo JWT secret. Set JWT_SECRET to a long random value before deploying.");
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessMs = accessMs;
    }

    public String generateToken(String email, String role, int tokenVersion) {
        Date now = new Date();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(ISSUER)
                .subject(email)
                .claim("typ", "access")
                .claim("role", role)
                .claim("tv", tokenVersion)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessMs))
                .signWith(key)
                .compact();
    }

    /** Verifies signature, expiry, issuer and token type. Throws JwtException if anything is off. */
    public Claims parseAccess(String token) {
        Claims c = Jwts.parser().verifyWith(key).requireIssuer(ISSUER).build()
                .parseSignedClaims(token).getPayload();
        if (!"access".equals(c.get("typ", String.class))) throw new JwtException("Not an access token");
        return c;
    }
}
