package com.company.leave_management_system.controller;

import com.company.leave_management_system.dto.ApiResponse;
import com.company.leave_management_system.dto.LeaveBalanceDTO;
import com.company.leave_management_system.dto.YearEndActionDTO;
import com.company.leave_management_system.service.LeaveBalanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/leave-balance")
@RequiredArgsConstructor
@Tag(name = "Leave Balance", description = "Leave balance and year-end settlement")
@SecurityRequirement(name = "bearerAuth")
@CrossOrigin(origins = "*")
public class LeaveBalanceController {

    private final LeaveBalanceService leaveBalanceService;

    @GetMapping("/employee/{employeeId}")
    @Operation(summary = "Get leave balance", description = "Get leave balance for an employee")
    public ResponseEntity<ApiResponse<LeaveBalanceDTO>> getLeaveBalance(
            @PathVariable Long employeeId,
            @RequestParam Integer year) {
        LeaveBalanceDTO balance = leaveBalanceService.getLeaveBalance(employeeId, year);
        return ResponseEntity.ok(ApiResponse.success("Leave balance retrieved successfully", balance));
    }

    @PostMapping("/employee/{employeeId}/year-end")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Process year-end action",
            description = "Process carry forward or encashment (mutually exclusive)")
    public ResponseEntity<ApiResponse<Void>> processYearEndAction(
            @PathVariable Long employeeId,
            @Valid @RequestBody YearEndActionDTO dto) {
        leaveBalanceService.processYearEndAction(employeeId, dto);
        return ResponseEntity.ok(ApiResponse.success("Year-end action processed successfully", null));
    }
}