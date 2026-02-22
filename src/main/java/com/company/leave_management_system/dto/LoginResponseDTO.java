package com.company.leave_management_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDTO {
    private String token;
    private String tokenType;
    private String username;
    private String role;
    private Long employeeId;
    private String employeeName;

    // NEW FIELD: Manager approval status
    private Boolean isApproved;
}