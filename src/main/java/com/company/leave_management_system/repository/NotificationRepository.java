package com.company.leave_management_system.repository;

import com.company.leave_management_system.entity.Notification;
import com.company.leave_management_system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    Long countByUserAndIsReadFalse(User user);
}
