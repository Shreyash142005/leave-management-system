package com.company.leave_management_system.dto;

import com.company.leave_management_system.enums.HalfDayType;
import com.company.leave_management_system.enums.LeaveDuration;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequestDTO {

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @NotBlank(message = "Reason cannot be empty")
    private String reason;

    @NotNull(message = "Duration is required")
    private LeaveDuration duration = LeaveDuration.FULL_DAY;

    private HalfDayType halfDayType; // Required only if duration is HALF_DAY
}
