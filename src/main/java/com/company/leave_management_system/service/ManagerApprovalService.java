package com.company.leave_management_system.service;

import com.company.leave_management_system.dto.ManagerApprovalDTO;
import com.company.leave_management_system.entity.User;
import com.company.leave_management_system.enums.Role;
import com.company.leave_management_system.repository.EmployeeRepository;
import com.company.leave_management_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ManagerApprovalService {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final EmailService emailService; // NEW: Inject email service
    private final NotificationService notificationService;

    /**
     * Get pending managers (not approved yet)
     */
    public Page<ManagerApprovalDTO> getPendingManagers(Pageable pageable) {
        Page<User> managers = userRepository.findByRoleAndIsApproved(
                Role.MANAGER,
                false,
                pageable
        );
        return managers.map(this::convertToDTO);
    }

    /**
     * Get all managers
     */
    public Page<ManagerApprovalDTO> getAllManagers(Pageable pageable) {
        Page<User> managers = userRepository.findByRole(Role.MANAGER, pageable);
        return managers.map(this::convertToDTO);
    }

    /**
     * Approve manager - grant permission to approve/reject leaves
     */
    @Transactional
    public void approveManager(Long userId) {
        User manager = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Manager not found"));

        if (manager.getRole() != Role.MANAGER) {
            throw new RuntimeException("User is not a manager");
        }

        if (manager.getIsApproved()) {
            throw new RuntimeException("Manager already approved");
        }

        String adminUsername = SecurityContextHolder.getContext().getAuthentication().getName();

        manager.setIsApproved(true);
        manager.setApprovedBy(adminUsername);
        manager.setApprovedAt(LocalDateTime.now());
        userRepository.save(manager);
        notificationService.createNotification(
        manager,
        "Your manager account has been approved by admin: " + adminUsername
            );

        // NEW: Send approval email to manager
        employeeRepository.findByUserId(manager.getId()).ifPresent(employee -> {
            emailService.sendManagerApprovedEmail(
                    employee.getEmail(),
                    employee.getName(),
                    adminUsername
            );
        });
    }

    /**
     * Reject manager - remove permission to approve/reject leaves
     */
    @Transactional
    public void rejectManager(Long userId) {
        User manager = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Manager not found"));

        if (manager.getRole() != Role.MANAGER) {
            throw new RuntimeException("User is not a manager");
        }

        manager.setIsApproved(false);
        manager.setApprovedBy(null);
        manager.setApprovedAt(null);
        userRepository.save(manager);
        notificationService.createNotification(
        manager,
        "Your manager approval has been revoked by admin."
);
    }

    /**
     * Convert User to DTO
     */
    private ManagerApprovalDTO convertToDTO(User user) {
        ManagerApprovalDTO dto = new ManagerApprovalDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setIsApproved(user.getIsApproved());
        dto.setApprovedBy(user.getApprovedBy());
        dto.setApprovedAt(user.getApprovedAt());
        dto.setCreatedAt(user.getCreatedAt());

        // Get employee details
        employeeRepository.findByUserId(user.getId()).ifPresent(employee -> {
            dto.setEmployeeName(employee.getName());
            dto.setEmployeeEmail(employee.getEmail());
            dto.setDepartment(employee.getDepartment());
        });

        return dto;
    }

}
