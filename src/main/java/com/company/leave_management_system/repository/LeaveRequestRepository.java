package com.company.leave_management_system.repository;

import com.company.leave_management_system.entity.LeaveRequest;
import com.company.leave_management_system.enums.LeaveStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    /**
     * Find leaves by employee ID with pagination
     */
    Page<LeaveRequest> findByEmployeeId(Long employeeId, Pageable pageable);

    /**
     * Find leaves by status with pagination
     */
    Page<LeaveRequest> findByStatus(LeaveStatus status, Pageable pageable);

    /**
     * NEW: Find leaves by employee's department with pagination
     * For managers to see only their department's leaves
     */
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.employee.department = :department")
    Page<LeaveRequest> findByEmployeeDepartment(
            @Param("department") String department,
            Pageable pageable);

    /**
     * NEW: Find leaves by employee's department and status with pagination
     * For managers to see only their department's leaves filtered by status
     */
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.employee.department = :department AND lr.status = :status")
    Page<LeaveRequest> findByEmployeeDepartmentAndStatus(
            @Param("department") String department,
            @Param("status") LeaveStatus status,
            Pageable pageable);

    /**
     * Check for overlapping leave requests
     */
    @Query("SELECT CASE WHEN COUNT(lr) > 0 THEN true ELSE false END " +
            "FROM LeaveRequest lr " +
            "WHERE lr.employee.id = :employeeId " +
            "AND lr.status IN :statuses " +
            "AND (:excludeId IS NULL OR lr.id != :excludeId) " +
            "AND ((lr.startDate <= :endDate AND lr.endDate >= :startDate))")
    boolean existsOverlapping(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludeId") Long excludeId,
            @Param("statuses") List<LeaveStatus> statuses);

    /**
     * Count auto-approved leaves in a specific month
     */
    @Query("SELECT COUNT(lr) FROM LeaveRequest lr " +
            "WHERE lr.employee.id = :employeeId " +
            "AND lr.status = 'APPROVED' " +
            "AND lr.workingDays <= 2 " +
            "AND MONTH(lr.createdAt) = :month " +
            "AND YEAR(lr.createdAt) = :year " +
            "AND lr.processedAt IS NOT NULL " +
            "AND TIMESTAMPDIFF(SECOND, lr.createdAt, lr.processedAt) < 5")
    long countAutoApprovedInMonth(
            @Param("employeeId") Long employeeId,
            @Param("month") int month,
            @Param("year") int year);
}