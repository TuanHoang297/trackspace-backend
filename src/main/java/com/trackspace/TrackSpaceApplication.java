package com.trackspace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * TrackSpace Backend Application
 * 
 * Main entry point for the TrackSpace backend application.
 * This is a monolithic Spring Boot application that provides:
 * - User & Group Management
 * - Jira Integration
 * - GitHub Integration
 * - Contribution Tracking & Analysis
 * - AI-Powered SRS Generation
 * - Notification System
 * 
 * @author TrackSpace Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableScheduling
public class TrackSpaceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrackSpaceApplication.class, args);
        System.out.println("\n========================================");
        System.out.println("🚀 TrackSpace Backend is running!");
        System.out.println("📖 Swagger UI: http://localhost:8080/swagger-ui.html");
        System.out.println("📝 API Docs: http://localhost:8080/api-docs");
        System.out.println("========================================\n");
    }
}
