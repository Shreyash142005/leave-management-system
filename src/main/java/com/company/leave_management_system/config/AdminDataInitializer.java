package com.company.leave_management_system.config;

import com.company.leave_management_system.entity.User;
import com.company.leave_management_system.enums.Role;
import com.company.leave_management_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Initializes default admin account on application startup
 * Username: admin
 * Password: admin123
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Check if admin already exists
        if (!userRepository.existsByUsername("admin")) {
            // Create default admin user
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(Role.ADMIN);
            admin.setEnabled(true);
            admin.setIsApproved(true);

            userRepository.save(admin);

            log.info("✅ Default admin account created successfully");
            log.info("   Username: admin");
            log.info("   Password: admin123");
            log.info("   ⚠️  Please change the password after first login!");
        } else {
            log.info("ℹ️  Default admin account already exists");
        }
    }
}