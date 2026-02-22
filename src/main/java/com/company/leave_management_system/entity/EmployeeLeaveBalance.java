package com.company.leave_management_system.entity;

import com.company.leave_management_system.enums.YearEndAction;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_leave_balance")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeLeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private Integer year;

    @Column(name = "total_entitlement", precision = 4, scale = 1)
    private BigDecimal totalEntitlement = BigDecimal.valueOf(24.0);

    @Column(name = "used_leaves", precision = 4, scale = 1)
    private BigDecimal usedLeaves = BigDecimal.ZERO;

    @Column(name = "remaining_leaves", precision = 4, scale = 1)
    private BigDecimal remainingLeaves = BigDecimal.valueOf(24.0);

    @Column(name = "carried_forward", precision = 4, scale = 1)
    private BigDecimal carriedForward = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "year_end_action", length = 20)
    private YearEndAction yearEndAction;

    @Column(name = "year_end_action_date")
    private LocalDateTime yearEndActionDate;

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