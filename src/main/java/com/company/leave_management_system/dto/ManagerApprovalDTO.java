package com.company.leave_management_system.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ManagerApprovalDTO {
    private Long id;
    private String username;
    private String employeeName;
    private String employeeEmail;
    private String department;
    private Boolean isApproved;
    private String approvedBy;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
}