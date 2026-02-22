package com.company.leave_management_system.service;

import com.company.leave_management_system.config.JwtTokenProvider;
import com.company.leave_management_system.dto.LoginRequestDTO;
import com.company.leave_management_system.dto.LoginResponseDTO;
import com.company.leave_management_system.dto.RegisterRequestDTO;
import com.company.leave_management_system.entity.Employee;
import com.company.leave_management_system.entity.EmployeeLeaveBalance;
import com.company.leave_management_system.entity.User;
import com.company.leave_management_system.enums.Role;
import com.company.leave_management_system.repository.EmployeeLeaveBalanceRepository;
import com.company.leave_management_system.repository.EmployeeRepository;
import com.company.leave_management_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeLeaveBalanceRepository leaveBalanceRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService; // NEW: Inject email service

    /**
     * Login - with manager approval check
     */
    public LoginResponseDTO login(LoginRequestDTO request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if user is enabled
        if (!user.getEnabled()) {
            throw new RuntimeException("Account is disabled");
        }

        // NEW: Check if manager is approved
        if (user.getRole() == Role.MANAGER && !user.getIsApproved()) {
            throw new RuntimeException("Manager account pending admin approval");
        }

        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole().name());

        LoginResponseDTO.LoginResponseDTOBuilder builder = LoginResponseDTO.builder()
                .token(token)
                .tokenType("Bearer")
                .username(user.getUsername())
                .role(user.getRole().name())
                .isApproved(user.getIsApproved()); // NEW: Include approval status

        // Add employee info if role is EMPLOYEE
        employeeRepository.findByUserId(user.getId()).ifPresent(employee -> {
            builder.employeeId(employee.getId());
            builder.employeeName(employee.getName());
        });

        return builder.build();
    }

    /**
     * Register - set approval based on role
     * IMPORTANT: Only one admin allowed in system (username: admin, password: admin123)
     */
    @Transactional
    public void register(RegisterRequestDTO request) {
        // Prevent registration of additional admins
        if (request.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("Admin registration is not allowed. Please contact system administrator.");
        }

        // Check if username exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Check if email exists
        if (employeeRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Create user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setEnabled(true);

        // NEW: Set approval status based on role
        if (request.getRole() == Role.EMPLOYEE) {
            user.setIsApproved(true); // Auto-approve EMPLOYEE
        } else if (request.getRole() == Role.MANAGER) {
            user.setIsApproved(false); // MANAGER needs admin approval
        }

        User savedUser = userRepository.save(user);

        // Create employee
        Employee employee = new Employee();
        employee.setUser(savedUser);
        employee.setName(request.getName());
        employee.setEmail(request.getEmail());
        employee.setDepartment(request.getDepartment());

        Employee savedEmployee = employeeRepository.save(employee);

        // Initialize leave balance for current year
        int currentYear = LocalDateTime.now().getYear();
        EmployeeLeaveBalance balance = new EmployeeLeaveBalance();
        balance.setEmployee(savedEmployee);
        balance.setYear(currentYear);
        balance.setTotalEntitlement(BigDecimal.valueOf(24.0));
        balance.setUsedLeaves(BigDecimal.ZERO);
        balance.setRemainingLeaves(BigDecimal.valueOf(24.0));
        balance.setCarriedForward(BigDecimal.ZERO);

        leaveBalanceRepository.save(balance);

        // NEW: Send welcome email
        emailService.sendWelcomeEmail(
                request.getEmail(),
                request.getName(),
                request.getUsername(),
                request.getRole().name()
        );

        // NEW: If manager, notify admin
        if (request.getRole() == Role.MANAGER) {
            // Find admin user and send notification
            userRepository.findByRole(Role.ADMIN, org.springframework.data.domain.Pageable.unpaged())
                    .stream()
                    .findFirst()
                    .flatMap(admin -> employeeRepository.findByUserId(admin.getId()))
                    .ifPresent(adminEmployee -> {
                        emailService.sendManagerApprovalNotification(
                                adminEmployee.getEmail(),
                                request.getName(),
                                request.getEmail()
                        );
                    });
        }
    }
}