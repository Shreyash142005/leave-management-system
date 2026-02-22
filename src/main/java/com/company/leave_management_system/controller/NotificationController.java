package com.company.leave_management_system.controller;

import com.company.leave_management_system.entity.Notification;
import com.company.leave_management_system.service.NotificationService;
import lombok.RequiredArgsConstructor;
import java.security.Principal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
public List<Notification> getMyNotifications(Principal principal) {
    User user = userService.findByUsername(principal.getName());
    return notificationService.getUserNotifications(user);
}

@GetMapping("/unread-count")
public Long getUnreadCount(Principal principal) {
    User user = userService.findByUsername(principal.getName());
    return notificationService.getUnreadCount(user);
}

    @PutMapping("/{id}/read")
    public void markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
    }
}
