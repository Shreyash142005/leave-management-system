package com.company.leave_management_system.service;

import com.company.leave_management_system.dto.LeaveRequestDTO;
import com.company.leave_management_system.dto.LeaveResponseDTO;
import com.company.leave_management_system.entity.Employee;
import com.company.leave_management_system.entity.LeaveRequest;
import com.company.leave_management_system.entity.User;
import com.company.leave_management_system.enums.LeaveStatus;
import com.company.leave_management_system.enums.Role;
import com.company.leave_management_system.exception.*;
import com.company.leave_management_system.repository.LeaveRequestRepository;
import com.company.leave_management_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeService employeeService;
    private final WorkingDayCalculator workingDayCalculator;
    private final LeaveBalanceService leaveBalanceService;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Value("${app.leave.max-auto-approvals-per-month:2}")
    private int maxAutoApprovalsPerMonth;

    @Value("${app.leave.auto-approval-threshold:2}")
    private int autoApprovalThreshold;

    /**
     * Get all leaves with pagination (ADMIN/MANAGER)
     * MANAGERS can only see leaves from their own department
     * ADMINS can see all leaves
     */
    public Page<LeaveResponseDTO> getAllLeaves(int page, int size, String sortBy, LeaveStatus status) {
        Sort sort = Sort.by(Sort.Direction.DESC, sortBy != null ? sortBy : "createdAt");
        Pageable pageable = PageRequest.of(page, size, sort);

        User currentUser = getCurrentUser();
        Employee currentEmployee = employeeService.getEmployeeByUserId(currentUser.getId());

        Page<LeaveRequest> leavePage;

        // ADMIN can see all leaves
        if (currentUser.getRole() == Role.ADMIN) {
            if (status != null) {
                leavePage = leaveRequestRepository.findByStatus(status, pageable);
            } else {
                leavePage = leaveRequestRepository.findAll(pageable);
            }
        }
        // MANAGER can only see leaves from their department
        else if (currentUser.getRole() == Role.MANAGER) {
            String managerDepartment = currentEmployee.getDepartment();

            if (managerDepartment == null || managerDepartment.trim().isEmpty()) {
                throw new IllegalStateException("Manager must be assigned to a department");
            }

            if (status != null) {
                leavePage = leaveRequestRepository.findByEmployeeDepartmentAndStatus(
                        managerDepartment, status, pageable);
            } else {
                leavePage = leaveRequestRepository.findByEmployeeDepartment(
                        managerDepartment, pageable);
            }

            log.info("Manager {} from {} department viewing leaves",
                    currentUser.getUsername(), managerDepartment);
        }
        else {
            throw new AccessDeniedException("Only ADMIN and MANAGER can view all leaves");
        }

        return leavePage.map(this::mapToResponseDTO);
    }

    /**
     * Approve leave (ADMIN/MANAGER only)
     * MANAGER can only approve leaves from their own department
     */
    @Transactional
    public LeaveResponseDTO approveLeave(Long id) {
        LeaveRequest leave = findLeaveById(id);

        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new InvalidLeaveRequestException(
                    "Only PENDING leaves can be approved. Current status: " + leave.getStatus());
        }

        User currentUser = getCurrentUser();

        // Check department-based access for MANAGER
        if (currentUser.getRole() == Role.MANAGER) {
            Employee currentEmployee = employeeService.getEmployeeByUserId(currentUser.getId());
            String managerDepartment = currentEmployee.getDepartment();
            String employeeDepartment = leave.getEmployee().getDepartment();

            if (!managerDepartment.equals(employeeDepartment)) {
                throw new AccessDeniedException(
                        String.format("You can only approve leaves from your department (%s). " +
                                        "This leave is from %s department.",
                                managerDepartment, employeeDepartment));
            }

            log.info("Manager {} from {} department approving leave for employee from same department",
                    currentUser.getUsername(), managerDepartment);
        }

        leave.setStatus(LeaveStatus.APPROVED);
        leave.setProcessedAt(LocalDateTime.now());
        leave.setProcessedBy(currentUser);

        LeaveRequest updated = leaveRequestRepository.save(leave);
        notificationService.createNotification(
        leave.getEmployee().getUser(),
        "Your leave request from " + leave.getStartDate() +
        " to " + leave.getEndDate() + " has been rejected."
        );

        // Send approval email
        emailService.sendLeaveApprovedEmail(updated);

        return mapToResponseDTO(updated);
    }

    /**
     * Reject leave (ADMIN/MANAGER only)
     * MANAGER can only reject leaves from their own department
     */
    @Transactional
    public LeaveResponseDTO rejectLeave(Long id) {
        LeaveRequest leave = findLeaveById(id);

        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new InvalidLeaveRequestException(
                    "Only PENDING leaves can be rejected. Current status: " + leave.getStatus());
        }

        User currentUser = getCurrentUser();

        // Check department-based access for MANAGER
        if (currentUser.getRole() == Role.MANAGER) {
            Employee currentEmployee = employeeService.getEmployeeByUserId(currentUser.getId());
            String managerDepartment = currentEmployee.getDepartment();
            String employeeDepartment = leave.getEmployee().getDepartment();

            if (!managerDepartment.equals(employeeDepartment)) {
                throw new AccessDeniedException(
                        String.format("You can only reject leaves from your department (%s). " +
                                        "This leave is from %s department.",
                                managerDepartment, employeeDepartment));
            }

            log.info("Manager {} from {} department rejecting leave for employee from same department",
                    currentUser.getUsername(), managerDepartment);
        }

        leave.setStatus(LeaveStatus.REJECTED);
        leave.setProcessedAt(LocalDateTime.now());
        leave.setProcessedBy(currentUser);

        LeaveRequest updated = leaveRequestRepository.save(leave);
        notificationService.createNotification(
        leave.getProcessedBy() != null ? leave.getProcessedBy() : null,
        currentEmployee.getName() + " has cancelled their leave request."
        );

        // Restore leave balance
        int year = leave.getStartDate().getYear();
        leaveBalanceService.restoreLeave(leave.getEmployee().getId(), leave.getWorkingDays(), year);

        // Send rejection email
        emailService.sendLeaveRejectedEmail(updated);

        return mapToResponseDTO(updated);
    }

    /**
     * Apply for leave (EMPLOYEE)
     */
    @Transactional
    public LeaveResponseDTO applyLeave(LeaveRequestDTO dto) {
        Employee employee = getCurrentEmployee();

        validateLeaveRequest(dto, employee.getId(), null);

        BigDecimal totalDays = workingDayCalculator.calculateTotalDays(
                dto.getStartDate(), dto.getEndDate(), dto.getDuration());
        BigDecimal workingDays = workingDayCalculator.calculateWorkingDays(
                dto.getStartDate(), dto.getEndDate(), dto.getDuration());

        int year = dto.getStartDate().getYear();
        leaveBalanceService.deductLeave(employee.getId(), workingDays, year);

        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setEmployee(employee);
        leaveRequest.setStartDate(dto.getStartDate());
        leaveRequest.setEndDate(dto.getEndDate());
        leaveRequest.setTotalDays(totalDays);
        leaveRequest.setWorkingDays(workingDays);
        leaveRequest.setReason(dto.getReason());
        leaveRequest.setDuration(dto.getDuration());
        leaveRequest.setHalfDayType(dto.getHalfDayType());

        boolean shouldAutoApprove = checkAutoApproval(employee.getId(), workingDays, dto.getStartDate());

        if (shouldAutoApprove) {
            leaveRequest.setStatus(LeaveStatus.APPROVED);
            leaveRequest.setProcessedAt(LocalDateTime.now());
            log.info("Leave auto-approved for employee: {}", employee.getId());
        } else {
            leaveRequest.setStatus(LeaveStatus.PENDING);
        }

        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);
        // Notify all managers of the same department
        List<User> managers = userRepository.findByRole(Role.MANAGER);
        
        for (User manager : managers) {
            notificationService.createNotification(
                    manager,
                    "New leave request submitted by " + employee.getName()
            );
        }
        emailService.sendLeaveAppliedEmail(saved);

        return mapToResponseDTO(saved);
    }

    /**
     * Get leaves by employee (EMPLOYEE can see own, ADMIN/MANAGER can see any)
     */
    public Page<LeaveResponseDTO> getLeavesByEmployee(Long employeeId, int page, int size, String sortBy) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"))) {
            Employee currentEmployee = getCurrentEmployee();
            if (!currentEmployee.getId().equals(employeeId)) {
                throw new AccessDeniedException("You can only view your own leaves");
            }
        }

        Sort sort = Sort.by(Sort.Direction.DESC, sortBy != null ? sortBy : "createdAt");
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<LeaveRequest> leavePage = leaveRequestRepository.findByEmployeeId(employeeId, pageable);
        return leavePage.map(this::mapToResponseDTO);
    }

    /**
     * Get leave by ID
     */
    public LeaveResponseDTO getLeaveById(Long id) {
        LeaveRequest leave = findLeaveById(id);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"))) {
            Employee currentEmployee = getCurrentEmployee();
            if (!leave.getEmployee().getId().equals(currentEmployee.getId())) {
                throw new AccessDeniedException("You can only view your own leaves");
            }
        }

        return mapToResponseDTO(leave);
    }

    /**
     * Cancel leave (EMPLOYEE can cancel own PENDING or future leaves)
     */
    @Transactional
    public LeaveResponseDTO cancelLeave(Long id) {
        LeaveRequest leave = findLeaveById(id);
        Employee currentEmployee = getCurrentEmployee();

        if (!leave.getEmployee().getId().equals(currentEmployee.getId())) {
            throw new AccessDeniedException("You can only cancel your own leaves");
        }

        if (leave.getStatus() != LeaveStatus.PENDING && leave.getStartDate().isBefore(LocalDate.now())) {
            throw new InvalidLeaveRequestException(
                    "You can only cancel pending leaves or leaves that haven't started yet");
        }

        leave.setStatus(LeaveStatus.CANCELLED);
        LeaveRequest updated = leaveRequestRepository.save(leave);
        notificationService.createNotification(
        leave.getEmployee().getUser(),
        "Your leave request from " + leave.getStartDate() +
        " to " + leave.getEndDate() + " has been approved."
        );

        int year = leave.getStartDate().getYear();
        leaveBalanceService.restoreLeave(leave.getEmployee().getId(), leave.getWorkingDays(), year);

        emailService.sendLeaveCancelledEmail(updated);

        return mapToResponseDTO(updated);
    }

    private void validateLeaveRequest(LeaveRequestDTO dto, Long employeeId, Long excludeLeaveId) {
        if (dto.getStartDate().isBefore(LocalDate.now())) {
            throw new InvalidLeaveRequestException("Start date cannot be before today");
        }

        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new InvalidLeaveRequestException("End date must be after or equal to start date");
        }

        if (dto.getReason() == null || dto.getReason().trim().isEmpty()) {
            throw new InvalidLeaveRequestException("Reason cannot be empty");
        }

        boolean hasOverlap = leaveRequestRepository.existsOverlapping(
                employeeId,
                dto.getStartDate(),
                dto.getEndDate(),
                excludeLeaveId,
                List.of(LeaveStatus.APPROVED, LeaveStatus.PENDING)
        );

        if (hasOverlap) {
            throw new LeaveOverlapException("Leave dates overlap with existing leave request");
        }
    }

    private boolean checkAutoApproval(Long employeeId, BigDecimal workingDays, LocalDate startDate) {
        if (workingDays.compareTo(BigDecimal.valueOf(autoApprovalThreshold)) > 0) {
            return false;
        }

        int month = startDate.getMonthValue();
        int year = startDate.getYear();

        long autoApprovedCount = leaveRequestRepository.countAutoApprovedInMonth(employeeId, month, year);

        return autoApprovedCount < maxAutoApprovalsPerMonth;
    }

    private LeaveRequest findLeaveById(Long id) {
        return leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with id: " + id));
    }

    private Employee getCurrentEmployee() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return employeeService.getEmployeeByUserId(user.getId());
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private LeaveResponseDTO mapToResponseDTO(LeaveRequest leave) {
        boolean canCancel = leave.getStatus() == LeaveStatus.PENDING ||
                (leave.getStatus() == LeaveStatus.APPROVED && leave.getStartDate().isAfter(LocalDate.now()));

        return LeaveResponseDTO.builder()
                .id(leave.getId())
                .employeeId(leave.getEmployee().getId())
                .employeeName(leave.getEmployee().getName())
                .employeeEmail(leave.getEmployee().getEmail())
                .startDate(leave.getStartDate())
                .endDate(leave.getEndDate())
                .totalDays(leave.getTotalDays())
                .workingDays(leave.getWorkingDays())
                .reason(leave.getReason())
                .status(leave.getStatus())
                .duration(leave.getDuration())
                .halfDayType(leave.getHalfDayType())
                .processedAt(leave.getProcessedAt())
                .processedBy(leave.getProcessedBy() != null ? leave.getProcessedBy().getUsername() : null)
                .createdAt(leave.getCreatedAt())
                .canCancel(canCancel)
                .build();
    }

}

