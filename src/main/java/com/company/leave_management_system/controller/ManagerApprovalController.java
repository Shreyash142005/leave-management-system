package com.company.leave_management_system.controller;

import com.company.leave_management_system.dto.ApiResponse;
import com.company.leave_management_system.dto.ManagerApprovalDTO;
import com.company.leave_management_system.service.ManagerApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/manager-approvals")
@RequiredArgsConstructor
@Tag(name = "Manager Approvals", description = "Admin approval for managers")
@SecurityRequirement(name = "bearerAuth")
public class ManagerApprovalController {

    private final ManagerApprovalService managerApprovalService;

    /**
     * Get pending manager approvals (Admin only)
     */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get pending manager approvals", description = "Admin can view all pending manager registrations")
    public ResponseEntity<ApiResponse<Page<ManagerApprovalDTO>>> getPendingManagers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortBy));
        Page<ManagerApprovalDTO> managers = managerApprovalService.getPendingManagers(pageable);
        return ResponseEntity.ok(ApiResponse.success("Pending managers retrieved", managers));
    }

    /**
     * Get all managers with their approval status
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all managers", description = "Admin can view all managers")
    public ResponseEntity<ApiResponse<Page<ManagerApprovalDTO>>> getAllManagers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortBy));
        Page<ManagerApprovalDTO> managers = managerApprovalService.getAllManagers(pageable);
        return ResponseEntity.ok(ApiResponse.success("All managers retrieved", managers));
    }

    /**
     * Approve manager
     */
    @PutMapping("/{managerId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve manager", description = "Admin approves a manager to allow them to approve/reject leaves")
    public ResponseEntity<ApiResponse<Void>> approveManager(
            @PathVariable Long managerId
    ) {
        managerApprovalService.approveManager(managerId);
        return ResponseEntity.ok(ApiResponse.<Void>success("Manager approved successfully"));
    }

    /**
     * Reject manager
     */
    @PutMapping("/{managerId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reject manager", description = "Admin rejects a manager - they cannot approve/reject leaves")
    public ResponseEntity<ApiResponse<Void>> rejectManager(
            @PathVariable Long managerId
    ) {
        managerApprovalService.rejectManager(managerId);
        return ResponseEntity.ok(ApiResponse.<Void>success("Manager rejected - approval permissions removed"));
    }
}