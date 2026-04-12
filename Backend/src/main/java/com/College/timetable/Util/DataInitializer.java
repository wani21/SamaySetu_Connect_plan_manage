package com.College.timetable.Util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.College.timetable.Entity.TeacherEntity;
import com.College.timetable.Repository.Teacher_Repo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class DataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    // In dev, these have safe defaults for local testing.
    // In prod, these MUST be set via environment variables — empty = skip admin creation.
    @Value("${app.admin.email:#{null}}")
    private String adminEmail;

    @Value("${app.admin.password:#{null}}")
    private String adminPassword;

    @Value("${app.admin.name:Admin User}")
    private String adminName;

    @Value("${app.admin.employee-id:EMP00001}")
    private String adminEmployeeId;

    @Bean
    CommandLineRunner initDatabase(Teacher_Repo teacherRepo, PasswordEncoder passwordEncoder) {
        return args -> {
            // Skip admin creation if credentials not provided (production safety)
            if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
                logger.warn("[INIT] Admin email/password not configured. Skipping admin user creation. " +
                    "Set APP_ADMIN_EMAIL and APP_ADMIN_PASSWORD environment variables to create an admin.");
                return;
            }

            // Check if admin already exists
            if (teacherRepo.findByEmail(adminEmail).isEmpty()) {
                TeacherEntity admin = new TeacherEntity();
                admin.setName(adminName);
                admin.setEmail(adminEmail);
                admin.setEmployeeId(adminEmployeeId);
                admin.setPassword(passwordEncoder.encode(adminPassword));
                admin.setRole("ADMIN");
                admin.setIsActive(true);
                admin.setIsApproved(true);
                admin.setIsEmailVerified(true);
                admin.setIsFirstLogin(false);
                admin.setMinWeeklyHours(10);
                admin.setMaxWeeklyHours(40);

                teacherRepo.save(admin);
                logger.info("[INIT] Admin user created: {}", adminEmail);
            } else {
                logger.debug("[INIT] Admin user already exists, skipping");
            }
        };
    }
}
