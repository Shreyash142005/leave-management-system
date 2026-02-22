package com.company.leave_management_system.exception;

public class LeaveOverlapException extends RuntimeException {
    public LeaveOverlapException(String message) {
        super(message);
    }
}
