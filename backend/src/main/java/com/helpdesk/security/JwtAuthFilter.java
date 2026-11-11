package com.helpdesk.security;

import com.helpdesk.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final UserRepository users;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                var claims = jwt.parseAccess(header.substring(7));
                Integer tv = claims.get("tv", Integer.class);
                users.findByEmail(claims.getSubject())
                        // deactivated users and tokens issued before a password change / sign-out-everywhere are rejected
                        .filter(u -> u.isActive() && tv != null && tv == u.getTokenVersion())
                        .ifPresent(u -> {
                            // role comes from the database, so role changes apply immediately
                            var auth = new UsernamePasswordAuthenticationToken(u.getEmail(), null,
                                    List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name())));
                            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                            SecurityContextHolder.getContext().setAuthentication(auth);
                        });
            } catch (JwtException | IllegalArgumentException ignored) {
                // invalid or expired token: continue unauthenticated, the entry point returns 401
            }
        }
        chain.doFilter(req, res);
    }
}
