package com.company.leave_management_system.dto;

import com.company.leave_management_system.enums.HalfDayType;
import com.company.leave_management_system.enums.LeaveDuration;
import com.company.leave_management_system.enums.LeaveStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveResponseDTO {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private String employeeEmail;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalDays;
    private BigDecimal workingDays;
    private String reason;
    private LeaveStatus status;
    private LeaveDuration duration;
    private HalfDayType halfDayType;
    private LocalDateTime processedAt;
    private String processedBy;
    private LocalDateTime createdAt;
    private boolean canCancel;
}