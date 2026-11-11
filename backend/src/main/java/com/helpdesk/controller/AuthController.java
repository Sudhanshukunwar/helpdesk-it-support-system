package com.helpdesk.controller;

import com.helpdesk.dto.AuthDtos.AuthResponse;
import com.helpdesk.dto.AuthDtos.LoginRequest;
import com.helpdesk.dto.AuthDtos.RefreshRequest;
import com.helpdesk.dto.AuthDtos.RegisterRequest;
import com.helpdesk.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService auth;

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest r) { return auth.register(r); }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest r) { return auth.login(r); }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest r) { return auth.refresh(r.refreshToken()); }

    @PostMapping("/logout")
    public void logout(@Valid @RequestBody RefreshRequest r) { auth.logout(r.refreshToken()); }
}
