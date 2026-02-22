package com.company.leave_management_system.service;

import com.company.leave_management_system.dto.LeaveBalanceDTO;
import com.company.leave_management_system.dto.YearEndActionDTO;
import com.company.leave_management_system.entity.Employee;
import com.company.leave_management_system.entity.EmployeeLeaveBalance;
import com.company.leave_management_system.enums.YearEndAction;
import com.company.leave_management_system.exception.InvalidLeaveRequestException;
import com.company.leave_management_system.exception.ResourceNotFoundException;
import com.company.leave_management_system.repository.EmployeeLeaveBalanceRepository;
import com.company.leave_management_system.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LeaveBalanceService {

    private final EmployeeLeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeRepository employeeRepository;

    @Value("${app.leave.annual-entitlement:24}")
    private int annualEntitlement;

    @Value("${app.leave.carry-forward-max:12}")
    private int carryForwardMax;

    @Value("${app.leave.encashment-max:10}")
    private int encashmentMax;

    public LeaveBalanceDTO getLeaveBalance(Long employeeId, Integer year) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        EmployeeLeaveBalance balance = leaveBalanceRepository
                .findByEmployeeIdAndYear(employeeId, year)
                .orElseGet(() -> createDefaultBalance(employee, year));

        return mapToDTO(balance);
    }

    @Transactional
    public EmployeeLeaveBalance getOrCreateBalance(Long employeeId, Integer year) {
        return leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, year)
                .orElseGet(() -> {
                    Employee employee = employeeRepository.findById(employeeId)
                            .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
                    return createDefaultBalance(employee, year);
                });
    }

    private EmployeeLeaveBalance createDefaultBalance(Employee employee, Integer year) {
        EmployeeLeaveBalance balance = new EmployeeLeaveBalance();
        balance.setEmployee(employee);
        balance.setYear(year);
        balance.setTotalEntitlement(BigDecimal.valueOf(annualEntitlement));
        balance.setUsedLeaves(BigDecimal.ZERO);
        balance.setRemainingLeaves(BigDecimal.valueOf(annualEntitlement));
        balance.setCarriedForward(BigDecimal.ZERO);
        return leaveBalanceRepository.save(balance);
    }

    @Transactional
    public void deductLeave(Long employeeId, BigDecimal workingDays, Integer year) {
        EmployeeLeaveBalance balance = getOrCreateBalance(employeeId, year);

        if (balance.getRemainingLeaves().compareTo(workingDays) < 0) {
            throw new InvalidLeaveRequestException(
                    String.format("Insufficient leave balance. Available: %.1f, Required: %.1f",
                            balance.getRemainingLeaves(), workingDays)
            );
        }

        balance.setUsedLeaves(balance.getUsedLeaves().add(workingDays));
        balance.setRemainingLeaves(balance.getRemainingLeaves().subtract(workingDays));
        leaveBalanceRepository.save(balance);
    }

    @Transactional
    public void restoreLeave(Long employeeId, BigDecimal workingDays, Integer year) {
        EmployeeLeaveBalance balance = getOrCreateBalance(employeeId, year);

        balance.setUsedLeaves(balance.getUsedLeaves().subtract(workingDays));
        balance.setRemainingLeaves(balance.getRemainingLeaves().add(workingDays));
        leaveBalanceRepository.save(balance);
    }

    @Transactional
    public void processYearEndAction(Long employeeId, YearEndActionDTO dto) {
        EmployeeLeaveBalance balance = leaveBalanceRepository
                .findByEmployeeIdAndYear(employeeId, dto.getYear())
                .orElseThrow(() -> new ResourceNotFoundException("Leave balance not found for year"));

        // Check if action already processed
        if (balance.getYearEndAction() != null) {
            throw new InvalidLeaveRequestException(
                    "Year-end action already processed for year " + dto.getYear()
            );
        }

        BigDecimal remaining = balance.getRemainingLeaves();

        if (dto.getAction() == YearEndAction.CARRY_FORWARD) {
            // Carry forward max 12 leaves
            BigDecimal toCarry = remaining.min(BigDecimal.valueOf(carryForwardMax));

            // Create or update next year balance
            EmployeeLeaveBalance nextYearBalance = getOrCreateBalance(employeeId, dto.getYear() + 1);
            nextYearBalance.setCarriedForward(toCarry);
            nextYearBalance.setTotalEntitlement(BigDecimal.valueOf(annualEntitlement).add(toCarry));
            nextYearBalance.setRemainingLeaves(nextYearBalance.getTotalEntitlement().subtract(nextYearBalance.getUsedLeaves()));
            leaveBalanceRepository.save(nextYearBalance);

        } else if (dto.getAction() == YearEndAction.ENCASHMENT) {
            // Encash max 10 leaves
            BigDecimal toEncash = remaining.min(BigDecimal.valueOf(encashmentMax));
            // Here you would typically integrate with payroll system
            // For now, we just record the action
        }

        balance.setYearEndAction(dto.getAction());
        balance.setYearEndActionDate(LocalDateTime.now());
        leaveBalanceRepository.save(balance);
    }

    private LeaveBalanceDTO mapToDTO(EmployeeLeaveBalance balance) {
        return LeaveBalanceDTO.builder()
                .id(balance.getId())
                .employeeId(balance.getEmployee().getId())
                .employeeName(balance.getEmployee().getName())
                .year(balance.getYear())
                .totalEntitlement(balance.getTotalEntitlement())
                .usedLeaves(balance.getUsedLeaves())
                .remainingLeaves(balance.getRemainingLeaves())
                .carriedForward(balance.getCarriedForward())
                .yearEndAction(balance.getYearEndAction())
                .canProcessYearEnd(balance.getYearEndAction() == null)
                .build();
    }
}