package com.company.leave_management_system.dto;

import com.company.leave_management_system.enums.YearEndAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveBalanceDTO {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private Integer year;
    private BigDecimal totalEntitlement;
    private BigDecimal usedLeaves;
    private BigDecimal remainingLeaves;
    private BigDecimal carriedForward;
    private YearEndAction yearEndAction;
    private boolean canProcessYearEnd;
}
