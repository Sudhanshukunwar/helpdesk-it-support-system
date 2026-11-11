package com.helpdesk.controller;

import com.helpdesk.dto.AuthDtos.ChangePasswordRequest;
import com.helpdesk.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {
    private final AuthService auth;

    @PutMapping("/password")
    public void changePassword(@Valid @RequestBody ChangePasswordRequest r, Authentication a) {
        auth.changePassword(a.getName(), r.currentPassword(), r.newPassword());
    }

    @PostMapping("/sign-out-everywhere")
    public void signOutEverywhere(Authentication a) { auth.signOutEverywhere(a.getName()); }
}
