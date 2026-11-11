package com.helpdesk.service;

import com.helpdesk.dto.AuthDtos.AuthResponse;
import com.helpdesk.dto.AuthDtos.LoginRequest;
import com.helpdesk.dto.AuthDtos.RegisterRequest;
import com.helpdesk.entity.Role;
import com.helpdesk.entity.User;
import com.helpdesk.exception.BadRequestException;
import com.helpdesk.repository.UserRepository;
import com.helpdesk.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtService jwt;

    public AuthResponse register(RegisterRequest r) {
        if (users.existsByEmail(r.email())) throw new BadRequestException("Email already registered");
        // Public registration always creates EMPLOYEE; admins promote users later.
        var u = users.save(User.builder().name(r.name()).email(r.email())
                .password(encoder.encode(r.password())).role(Role.EMPLOYEE).build());
        return toResponse(u);
    }

    public AuthResponse login(LoginRequest r) {
        authManager.authenticate(new UsernamePasswordAuthenticationToken(r.email(), r.password()));
        return toResponse(users.findByEmail(r.email()).orElseThrow());
    }

    private AuthResponse toResponse(User u) {
        return new AuthResponse(jwt.generateToken(u.getEmail(), u.getRole().name()),
                u.getId(), u.getName(), u.getEmail(), u.getRole());
    }
}
