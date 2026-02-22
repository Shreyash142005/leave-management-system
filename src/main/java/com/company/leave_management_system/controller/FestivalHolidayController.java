package com.company.leave_management_system.controller;

import com.company.leave_management_system.dto.ApiResponse;
import com.company.leave_management_system.dto.FestivalHolidayDTO;
import com.company.leave_management_system.service.FestivalHolidayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/holidays")
@RequiredArgsConstructor
@Tag(name = "Festival Holidays", description = "Manage company festival holidays")
@SecurityRequirement(name = "bearerAuth")
@CrossOrigin(origins = "*")
public class FestivalHolidayController {

    private final FestivalHolidayService festivalHolidayService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Add holiday", description = "Add a new festival holiday")
    public ResponseEntity<ApiResponse<FestivalHolidayDTO>> createHoliday(@Valid @RequestBody FestivalHolidayDTO dto) {
        FestivalHolidayDTO created = festivalHolidayService.createHoliday(dto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Holiday added successfully", created));
    }

    @GetMapping
    @Operation(summary = "Get all holidays", description = "Get all festival holidays")
    public ResponseEntity<ApiResponse<List<FestivalHolidayDTO>>> getAllHolidays() {
        List<FestivalHolidayDTO> holidays = festivalHolidayService.getAllHolidays();
        return ResponseEntity.ok(ApiResponse.success("Holidays retrieved successfully", holidays));
    }

    @GetMapping("/year/{year}")
    @Operation(summary = "Get holidays by year", description = "Get festival holidays for a specific year")
    public ResponseEntity<ApiResponse<List<FestivalHolidayDTO>>> getHolidaysByYear(@PathVariable Integer year) {
        List<FestivalHolidayDTO> holidays = festivalHolidayService.getHolidaysByYear(year);
        return ResponseEntity.ok(ApiResponse.success("Holidays retrieved successfully", holidays));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Delete holiday", description = "Delete a festival holiday")
    public ResponseEntity<ApiResponse<Void>> deleteHoliday(@PathVariable Long id) {
        festivalHolidayService.deleteHoliday(id);
        return ResponseEntity.ok(ApiResponse.success("Holiday deleted successfully", null));
    }
}
