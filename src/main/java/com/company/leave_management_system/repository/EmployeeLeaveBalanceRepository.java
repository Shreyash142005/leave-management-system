package com.company.leave_management_system.repository;

import com.company.leave_management_system.entity.EmployeeLeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeLeaveBalanceRepository extends JpaRepository<EmployeeLeaveBalance, Long> {

    Optional<EmployeeLeaveBalance> findByEmployeeIdAndYear(Long employeeId, Integer year);

    boolean existsByEmployeeIdAndYear(Long employeeId, Integer year);
}