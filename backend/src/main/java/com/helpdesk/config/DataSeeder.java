package com.helpdesk.config;

import com.helpdesk.entity.*;
import com.helpdesk.repository.SlaPolicyRepository;
import com.helpdesk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    private final UserRepository users;
    private final SlaPolicyRepository sla;
    private final PasswordEncoder encoder;

    @Override
    public void run(String... args) {
        if (!users.existsByEmail("admin@helpdesk.com")) {
            users.save(User.builder().name("Admin").email("admin@helpdesk.com")
                    .password(encoder.encode("admin123")).role(Role.ADMIN).build());
            users.save(User.builder().name("Support Agent").email("support@helpdesk.com")
                    .password(encoder.encode("support123")).role(Role.SUPPORT).build());
        }
        if (sla.count() == 0) {
            sla.save(new SlaPolicy(Priority.CRITICAL, 1, 4));
            sla.save(new SlaPolicy(Priority.HIGH, 2, 8));
            sla.save(new SlaPolicy(Priority.MEDIUM, 4, 24));
            sla.save(new SlaPolicy(Priority.LOW, 8, 72));
        }
    }
}
