package com.company.leave_management_system.entity;

import com.company.leave_management_system.enums.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    private Boolean enabled = true;

    /**
     * NEW FIELD: Manager approval status
     * - For ADMIN and EMPLOYEE: always true (no approval needed)
     * - For MANAGER: false until admin approves, true after approval
     */
    @Column(name = "is_approved", nullable = false)
    private Boolean isApproved = false;

    /**
     * NEW FIELD: Who approved this manager (only for MANAGER role)
     */
    @Column(name = "approved_by", length = 50)
    private String approvedBy;

    /**
     * NEW FIELD: When was this manager approved
     */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
