package com.helpdesk.dto;

import com.helpdesk.entity.Role;
import jakarta.validation.constraints.*;

public class AuthDtos {
    public static final String PASSWORD_RULE = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$";
    public static final String PASSWORD_MSG = "Password needs at least 8 characters, with a letter and a number";

    public record RegisterRequest(
            @NotBlank(message = "Name is required") String name,
            @Email(message = "Enter a valid email") @NotBlank(message = "Email is required") String email,
            @NotBlank(message = "Password is required") @Pattern(regexp = PASSWORD_RULE, message = PASSWORD_MSG) String password) {}

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record ChangePasswordRequest(
            @NotBlank(message = "Current password is required") String currentPassword,
            @NotBlank(message = "New password is required") @Pattern(regexp = PASSWORD_RULE, message = PASSWORD_MSG) String newPassword) {}

    public record AuthResponse(String token, String refreshToken, Long id, String name, String email, Role role) {}
}
