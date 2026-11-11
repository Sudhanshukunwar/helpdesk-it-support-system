package com.helpdesk.security;

import com.helpdesk.entity.Role;
import com.helpdesk.entity.User;
import com.helpdesk.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthFilterTest {
    final JwtService jwt = new JwtService("test-secret-test-secret-test-secret-123456", 60_000, false);
    final UserRepository users = mock(UserRepository.class);
    final JwtAuthFilter filter = new JwtAuthFilter(jwt, users);
    User user;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        user = User.builder().id(1L).name("Sam").email("sam@x.com").password("h").role(Role.SUPPORT).build();
        when(users.findByEmail("sam@x.com")).thenReturn(Optional.of(user));
    }

    @AfterEach
    void tearDown() { SecurityContextHolder.clearContext(); }

    private Authentication run(String token) throws Exception {
        var req = new MockHttpServletRequest();
        if (token != null) req.addHeader("Authorization", "Bearer " + token);
        filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Test
    void roleComesFromTheDatabaseNotFromTheToken() throws Exception {
        // token claims ADMIN, database says SUPPORT
        Authentication auth = run(jwt.generateToken("sam@x.com", "ADMIN", 0));
        assertNotNull(auth);
        assertEquals("sam@x.com", auth.getName());
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPPORT")));
        assertTrue(auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void tokenIssuedBeforeAPasswordChangeIsRejected() throws Exception {
        user.setTokenVersion(1);
        assertNull(run(jwt.generateToken("sam@x.com", "SUPPORT", 0)));
    }

    @Test
    void deactivatedUserIsRejectedEvenWithAValidToken() throws Exception {
        user.setActive(false);
        assertNull(run(jwt.generateToken("sam@x.com", "SUPPORT", 0)));
    }

    @Test
    void missingOrGarbageTokensStayUnauthenticated() throws Exception {
        assertNull(run(null));
        assertNull(run("not-a-jwt"));
    }
}
