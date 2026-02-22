package com.company.leave_management_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {
    private long totalEmployees;
    private long totalManagers;
    private long pendingLeaves;
    private long approvedLeaves;
    private long rejectedLeaves;
    private long pendingManagers;
    private long approvedManagers;
}