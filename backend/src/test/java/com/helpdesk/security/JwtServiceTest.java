package com.helpdesk.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    static final String SECRET = "test-secret-test-secret-test-secret-123456";
    final JwtService jwt = new JwtService(SECRET, 60_000, false);

    @Test
    void validTokenCarriesSubjectRoleAndVersion() {
        String token = jwt.generateToken("a@x.com", "ADMIN", 3);
        Claims c = jwt.parseAccess(token);
        assertEquals("a@x.com", c.getSubject());
        assertEquals("ADMIN", c.get("role", String.class));
        assertEquals(3, c.get("tv", Integer.class));
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        var other = new JwtService("another-secret-another-secret-123456789", 60_000, false);
        assertThrows(JwtException.class, () -> jwt.parseAccess(other.generateToken("a@x.com", "ADMIN", 0)));
    }

    @Test
    void expiredTokenIsRejected() {
        var expired = new JwtService(SECRET, -1_000, false);
        assertThrows(JwtException.class, () -> jwt.parseAccess(expired.generateToken("a@x.com", "ADMIN", 0)));
    }

    @Test
    void tokenOfAnotherTypeCannotBeUsedAsAccessToken() {
        String refreshLike = Jwts.builder().issuer("helpdesk").subject("a@x.com").claim("typ", "refresh")
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
        assertThrows(JwtException.class, () -> jwt.parseAccess(refreshLike));
    }

    @Test
    void tooShortSecretPreventsStartup() {
        assertThrows(IllegalStateException.class, () -> new JwtService("short", 1_000, false));
    }

    @Test
    void demoSecretIsRefusedInProduction() {
        assertThrows(IllegalStateException.class,
                () -> new JwtService("change-this-to-a-long-random-secret-key-123456", 1_000, true));
    }
}
