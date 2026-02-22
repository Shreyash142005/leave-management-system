package com.company.leave_management_system.controller;

import com.company.leave_management_system.dto.ApiResponse;
import com.company.leave_management_system.dto.LeaveRequestDTO;
import com.company.leave_management_system.dto.LeaveResponseDTO;
import com.company.leave_management_system.enums.LeaveStatus;
import com.company.leave_management_system.service.LeaveRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
@Tag(name = "Leave Management", description = "Leave request operations")
@SecurityRequirement(name = "bearerAuth")
@CrossOrigin(origins = "*")
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Apply for leave", description = "Submit a new leave request")
    public ResponseEntity<ApiResponse<LeaveResponseDTO>> applyLeave(@Valid @RequestBody LeaveRequestDTO dto) {
        LeaveResponseDTO response = leaveRequestService.applyLeave(dto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Leave request submitted successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get all leaves", description = "Get all leave requests with pagination")
    public ResponseEntity<ApiResponse<Page<LeaveResponseDTO>>> getAllLeaves(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) LeaveStatus status) {

        Page<LeaveResponseDTO> leaves = leaveRequestService.getAllLeaves(page, size, sortBy, status);
        return ResponseEntity.ok(ApiResponse.success("Leaves retrieved successfully", leaves));
    }

    @GetMapping("/employee/{employeeId}")
    @Operation(summary = "Get leaves by employee", description = "Get leave requests for a specific employee")
    public ResponseEntity<ApiResponse<Page<LeaveResponseDTO>>> getLeavesByEmployee(
            @PathVariable Long employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy) {

        Page<LeaveResponseDTO> leaves = leaveRequestService.getLeavesByEmployee(employeeId, page, size, sortBy);
        return ResponseEntity.ok(ApiResponse.success("Employee leaves retrieved successfully", leaves));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get leave by ID", description = "Get a specific leave request")
    public ResponseEntity<ApiResponse<LeaveResponseDTO>> getLeaveById(@PathVariable Long id) {
        LeaveResponseDTO leave = leaveRequestService.getLeaveById(id);
        return ResponseEntity.ok(ApiResponse.success("Leave retrieved successfully", leave));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Approve leave", description = "Approve a pending leave request")
    public ResponseEntity<ApiResponse<LeaveResponseDTO>> approveLeave(@PathVariable Long id) {
        LeaveResponseDTO approved = leaveRequestService.approveLeave(id);
        return ResponseEntity.ok(ApiResponse.success("Leave approved successfully", approved));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Reject leave", description = "Reject a pending leave request")
    public ResponseEntity<ApiResponse<LeaveResponseDTO>> rejectLeave(@PathVariable Long id) {
        LeaveResponseDTO rejected = leaveRequestService.rejectLeave(id);
        return ResponseEntity.ok(ApiResponse.success("Leave rejected successfully", rejected));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Cancel leave", description = "Cancel own leave request")
    public ResponseEntity<ApiResponse<LeaveResponseDTO>> cancelLeave(@PathVariable Long id) {
        LeaveResponseDTO cancelled = leaveRequestService.cancelLeave(id);
        return ResponseEntity.ok(ApiResponse.success("Leave cancelled successfully", cancelled));
    }
}