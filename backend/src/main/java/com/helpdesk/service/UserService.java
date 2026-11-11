package com.helpdesk.service;

import com.helpdesk.dto.Dtos.UpdateUserRequest;
import com.helpdesk.dto.Dtos.UserResponse;
import com.helpdesk.entity.Role;
import com.helpdesk.entity.User;
import com.helpdesk.exception.BadRequestException;
import com.helpdesk.exception.NotFoundException;
import com.helpdesk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository users;
    private final RefreshTokenService refreshTokens;

    @Transactional(readOnly = true)
    public List<UserResponse> all() { return users.findAll().stream().map(this::toDto).toList(); }

    @Transactional(readOnly = true)
    public List<UserResponse> byRole(Role role) { return users.findByRole(role).stream().map(this::toDto).toList(); }

    public UserResponse update(Long id, UpdateUserRequest r, String adminEmail) {
        var u = users.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
        if (u.getEmail().equals(adminEmail)) throw new BadRequestException("You can't change your own role or status");
        if (r.role() != null) u.setRole(r.role());
        if (r.active() != null) {
            u.setActive(r.active());
            if (!r.active()) {
                u.setTokenVersion(u.getTokenVersion() + 1);   // access tokens stop working at once
                refreshTokens.revokeAll(u.getId());           // and they cannot get new ones
            }
        }
        return toDto(u);
    }

    private UserResponse toDto(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.isActive());
    }
}
