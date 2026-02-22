package com.company.leave_management_system.repository;

import com.company.leave_management_system.entity.User;
import com.company.leave_management_system.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    /**
     * NEW: Find managers by approval status
     */
    Page<User> findByRoleAndIsApproved(Role role, Boolean isApproved, Pageable pageable);

    /**
     * NEW: Find all users by role
     */
    Page<User> findByRole(Role role, Pageable pageable);
}