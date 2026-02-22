package com.company.leave_management_system.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Configuration to enable asynchronous method execution
 * Required for @Async annotation in EmailService
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    // This enables @Async annotation for sending emails asynchronously
    // Emails will be sent in background threads without blocking the main request
}