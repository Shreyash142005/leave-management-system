package com.company.leave_management_system.dto;

import com.company.leave_management_system.enums.YearEndAction;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class YearEndActionDTO {

    @NotNull(message = "Year is required")
    private Integer year;

    @NotNull(message = "Action is required")
    private YearEndAction action;
}