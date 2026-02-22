-- Create Database
CREATE DATABASE IF NOT EXISTS leave_management_db;
USE leave_management_db;

-- User Table (Authentication)
CREATE TABLE IF NOT EXISTS user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CHECK (role IN ('EMPLOYEE', 'ADMIN', 'MANAGER'))
);

-- Employee Table
CREATE TABLE IF NOT EXISTS employee (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    department VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
);

-- Festival Holiday Table
CREATE TABLE IF NOT EXISTS festival_holiday (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    date DATE NOT NULL UNIQUE,
    year INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Leave Request Table
CREATE TABLE IF NOT EXISTS leave_request (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    total_days DECIMAL(4, 1) NOT NULL,
    working_days DECIMAL(4, 1) NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    duration VARCHAR(20) NOT NULL DEFAULT 'FULL_DAY',
    half_day_type VARCHAR(20),
    processed_at TIMESTAMP NULL,
    processed_by BIGINT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (employee_id) REFERENCES employee(id) ON DELETE CASCADE,
    FOREIGN KEY (processed_by) REFERENCES user(id) ON DELETE SET NULL,
    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    CHECK (duration IN ('FULL_DAY', 'HALF_DAY')),
    CHECK (half_day_type IN ('FIRST_HALF', 'SECOND_HALF', NULL))
);

-- Employee Leave Balance Table (Year-wise)
CREATE TABLE IF NOT EXISTS employee_leave_balance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    year INT NOT NULL,
    total_entitlement DECIMAL(4, 1) DEFAULT 24.0,
    used_leaves DECIMAL(4, 1) DEFAULT 0.0,
    remaining_leaves DECIMAL(4, 1) DEFAULT 24.0,
    carried_forward DECIMAL(4, 1) DEFAULT 0.0,
    year_end_action VARCHAR(20),
    year_end_action_date TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_employee_year (employee_id, year),
    FOREIGN KEY (employee_id) REFERENCES employee(id) ON DELETE CASCADE,
    CHECK (year_end_action IN ('CARRY_FORWARD', 'ENCASHMENT', NULL))
);

-- Email Log Table
CREATE TABLE IF NOT EXISTS email_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'SUCCESS',
    error_message TEXT,
    CHECK (status IN ('SUCCESS', 'FAILED'))
);

-- Indexes for Performance
CREATE INDEX idx_leave_employee ON leave_request(employee_id);
CREATE INDEX idx_leave_status ON leave_request(status);
CREATE INDEX idx_leave_dates ON leave_request(start_date, end_date);
CREATE INDEX idx_holiday_date ON festival_holiday(date);
CREATE INDEX idx_balance_employee_year ON employee_leave_balance(employee_id, year);

-- Migration SQL for Manager Approval Feature
-- Add new columns to user table (note: table name is 'user', not 'users')

-- Add isApproved column (default false for MANAGER, true for others)
ALTER TABLE user ADD COLUMN is_approved BOOLEAN NOT NULL DEFAULT false;

-- Add approvedBy column (stores admin username who approved the manager)
ALTER TABLE user ADD COLUMN approved_by VARCHAR(50);

-- Add approvedAt column (timestamp when manager was approved)
ALTER TABLE user ADD COLUMN approved_at DATETIME;

-- Update existing records: set is_approved=true for ADMIN and EMPLOYEE
UPDATE user SET is_approved = true WHERE role IN ('ADMIN', 'EMPLOYEE');

-- Update existing MANAGER records: set is_approved=true (for backward compatibility)
-- Comment out the line below if you want existing managers to require re-approval
UPDATE user SET is_approved = true WHERE role = 'MANAGER';

-- Insert Default Admin User (password: admin123)
INSERT INTO user (username, password, role, enabled) VALUES
('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN', TRUE),
('manager', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'MANAGER', TRUE);

-- Insert Sample Employees with Users (password: password123)
INSERT INTO user (username, password, role, enabled) VALUES
('john.doe', '$2a$10$lPC2birjuNlKXJLPcwJmbu3BXmRjZ.w8.k3rqH3xONqLdPLdBYICm', 'EMPLOYEE', TRUE),
('jane.smith', '$2a$10$lPC2birjuNlKXJLPcwJmbu3BXmRjZ.w8.k3rqH3xONqLdPLdBYICm', 'EMPLOYEE', TRUE),
('robert.johnson', '$2a$10$lPC2birjuNlKXJLPcwJmbu3BXmRjZ.w8.k3rqH3xONqLdPLdBYICm', 'EMPLOYEE', TRUE);

INSERT INTO employee (user_id, name, email, department) VALUES
(3, 'John Doe', 'john.doe@company.com', 'Engineering'),
(4, 'Jane Smith', 'jane.smith@company.com', 'Marketing'),
(5, 'Robert Johnson', 'robert.johnson@company.com', 'Sales');

INSERT INTO user (username, password, role, enabled, is_approved, created_at, updated_at)
SELECT 'admin',
       '$2a$10$xQGXZ3qN6bQ8qXJ8rJ8rJO9qXJ8rJ8rJO9qXJ8rJ8rJO9qXJ8rJ8rJ',
       'ADMIN',
       true,
       true,
       NOW(),
       NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM user WHERE username = 'admin'
);

-- Insert Festival Holidays for 2026
INSERT INTO festival_holiday (name, date, year) VALUES
('New Year', '2026-01-01', 2026),
('Republic Day', '2026-01-26', 2026),
('Holi', '2026-03-14', 2026),
('Good Friday', '2026-04-03', 2026),
('Independence Day', '2026-08-15', 2026),
('Gandhi Jayanti', '2026-10-02', 2026),
('Diwali', '2026-10-19', 2026),
('Christmas', '2026-12-25', 2026);

-- Initialize Leave Balance for 2026
INSERT INTO employee_leave_balance (employee_id, year, total_entitlement, used_leaves, remaining_leaves) VALUES
(1, 2026, 24.0, 0.0, 24.0),
(2, 2026, 24.0, 0.0, 24.0),
(3, 2026, 24.0, 0.0, 24.0);

--------------------------------------------------------------------------------
-- Trigger to update employee_leave_balance when leave status changes
--------------------------------------------------------------------------------
DELIMITER //

CREATE TRIGGER trg_leave_status_update
AFTER UPDATE ON leave_request
FOR EACH ROW
BEGIN
    DECLARE leave_diff DECIMAL(4,1);

    -- Only proceed if status changed
    IF OLD.status <> NEW.status THEN
        -- Calculate leave difference
        SET leave_diff = NEW.working_days;

        -- If leave approved, add to used_leaves
        IF NEW.status = 'APPROVED' THEN
            UPDATE employee_leave_balance
            SET used_leaves = used_leaves + leave_diff,
                remaining_leaves = total_entitlement - (used_leaves + leave_diff)
            WHERE employee_id = NEW.employee_id AND year = YEAR(NEW.start_date);
        END IF;

        -- If leave cancelled after approval, deduct from used_leaves
        IF OLD.status = 'APPROVED' AND NEW.status = 'CANCELLED' THEN
            UPDATE employee_leave_balance
            SET used_leaves = used_leaves - leave_diff,
                remaining_leaves = total_entitlement - (used_leaves - leave_diff)
            WHERE employee_id = NEW.employee_id AND year = YEAR(NEW.start_date);
        END IF;
    END IF;
END;
//

DELIMITER ;
