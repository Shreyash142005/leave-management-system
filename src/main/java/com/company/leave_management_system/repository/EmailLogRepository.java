package com.company.leave_management_system.repository;

import com.company.leave_management_system.entity.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {
    // Add custom methods if needed for email audit
}