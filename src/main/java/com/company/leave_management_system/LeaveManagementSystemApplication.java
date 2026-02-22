package com.company.leave_management_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LeaveManagementSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(LeaveManagementSystemApplication.class, args);
		System.out.println("\n===========================================");
		System.out.println("Leave Management System Started Successfully!");
		System.out.println("Swagger UI: http://localhost:8080/swagger-ui.html");
		System.out.println("Application: http://localhost:8080");
		System.out.println("===========================================\n");
	}
}