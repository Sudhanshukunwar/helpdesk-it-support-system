package com.helpdesk.controller;

import com.helpdesk.dto.Dtos.UpdateUserRequest;
import com.helpdesk.dto.Dtos.UserResponse;
import com.helpdesk.entity.Role;
import com.helpdesk.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {
    private final UserService svc;

    @GetMapping
    public List<UserResponse> all() { return svc.all(); }

    @GetMapping("/support")
    public List<UserResponse> support() { return svc.byRole(Role.SUPPORT); }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @RequestBody UpdateUserRequest r, Authentication a) {
        return svc.update(id, r, a.getName());
    }
}
