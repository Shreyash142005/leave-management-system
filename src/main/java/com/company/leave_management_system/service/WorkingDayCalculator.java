package com.company.leave_management_system.service;

import com.company.leave_management_system.enums.LeaveDuration;
import com.company.leave_management_system.exception.InvalidLeaveRequestException;
import com.company.leave_management_system.repository.FestivalHolidayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/**
 * Service to calculate working days excluding weekends and festival holidays
 */
@Service
@RequiredArgsConstructor
public class WorkingDayCalculator {

    private final FestivalHolidayRepository festivalHolidayRepository;

    /**
     * Calculate working days between two dates
     *
     * @param startDate Start date of leave
     * @param endDate End date of leave
     * @param duration FULL_DAY or HALF_DAY
     * @return Number of working days (0.5 for half day, integer for full days)
     */
    public BigDecimal calculateWorkingDays(LocalDate startDate, LocalDate endDate, LeaveDuration duration) {
        // For half-day leave
        if (duration == LeaveDuration.HALF_DAY) {
            // Validate: half-day must be on a working day
            if (isWeekend(startDate)) {
                throw new InvalidLeaveRequestException("Half-day leave cannot be on weekend");
            }

            // Get holidays in range
            List<LocalDate> holidays = festivalHolidayRepository.findHolidayDatesBetween(startDate, startDate);
            if (!holidays.isEmpty()) {
                throw new InvalidLeaveRequestException("Half-day leave cannot be on a festival holiday");
            }

            return BigDecimal.valueOf(0.5);
        }

        // For full-day leave, calculate working days
        BigDecimal workingDays = BigDecimal.ZERO;
        LocalDate current = startDate;

        // Get all holidays in the date range
        List<LocalDate> holidays = festivalHolidayRepository.findHolidayDatesBetween(startDate, endDate);

        while (!current.isAfter(endDate)) {
            // Count only if not weekend and not holiday
            if (!isWeekend(current) && !holidays.contains(current)) {
                workingDays = workingDays.add(BigDecimal.ONE);
            }
            current = current.plusDays(1);
        }

        // If no working days, reject the leave
        if (workingDays.compareTo(BigDecimal.ZERO) == 0) {
            throw new InvalidLeaveRequestException(
                    "Leave request has no working days. All days are weekends or holidays."
            );
        }

        return workingDays;
    }

    /**
     * Calculate total calendar days between two dates (inclusive)
     */
    public BigDecimal calculateTotalDays(LocalDate startDate, LocalDate endDate, LeaveDuration duration) {
        if (duration == LeaveDuration.HALF_DAY) {
            return BigDecimal.valueOf(0.5);
        }

        long days = endDate.toEpochDay() - startDate.toEpochDay() + 1;
        return BigDecimal.valueOf(days);
    }

    /**
     * Check if a date is weekend (Saturday or Sunday)
     */
    public boolean isWeekend(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    /**
     * Check if a date is a working day (not weekend and not holiday)
     */
    public boolean isWorkingDay(LocalDate date) {
        if (isWeekend(date)) {
            return false;
        }

        return !festivalHolidayRepository.existsByDate(date);
    }
}