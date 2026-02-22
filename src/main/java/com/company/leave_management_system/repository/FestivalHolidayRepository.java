package com.company.leave_management_system.repository;

import com.company.leave_management_system.entity.FestivalHoliday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FestivalHolidayRepository extends JpaRepository<FestivalHoliday, Long> {

    List<FestivalHoliday> findByYear(Integer year);

    boolean existsByDate(LocalDate date);

    @Query("SELECT h.date FROM FestivalHoliday h WHERE h.date BETWEEN :startDate AND :endDate")
    List<LocalDate> findHolidayDatesBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
