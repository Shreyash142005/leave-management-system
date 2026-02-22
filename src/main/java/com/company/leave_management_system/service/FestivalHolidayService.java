package com.company.leave_management_system.service;

import com.company.leave_management_system.dto.FestivalHolidayDTO;
import com.company.leave_management_system.entity.FestivalHoliday;
import com.company.leave_management_system.exception.ResourceNotFoundException;
import com.company.leave_management_system.repository.FestivalHolidayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FestivalHolidayService {

    private final FestivalHolidayRepository festivalHolidayRepository;

    @Transactional
    public FestivalHolidayDTO createHoliday(FestivalHolidayDTO dto) {
        if (festivalHolidayRepository.existsByDate(dto.getDate())) {
            throw new IllegalArgumentException("Holiday already exists for date: " + dto.getDate());
        }

        FestivalHoliday holiday = new FestivalHoliday();
        holiday.setName(dto.getName());
        holiday.setDate(dto.getDate());
        holiday.setYear(dto.getYear());

        FestivalHoliday saved = festivalHolidayRepository.save(holiday);
        return mapToDTO(saved);
    }

    public List<FestivalHolidayDTO> getAllHolidays() {
        return festivalHolidayRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<FestivalHolidayDTO> getHolidaysByYear(Integer year) {
        return festivalHolidayRepository.findByYear(year).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteHoliday(Long id) {
        if (!festivalHolidayRepository.existsById(id)) {
            throw new ResourceNotFoundException("Holiday not found with id: " + id);
        }
        festivalHolidayRepository.deleteById(id);
    }

    private FestivalHolidayDTO mapToDTO(FestivalHoliday holiday) {
        return FestivalHolidayDTO.builder()
                .id(holiday.getId())
                .name(holiday.getName())
                .date(holiday.getDate())
                .year(holiday.getYear())
                .build();
    }
}