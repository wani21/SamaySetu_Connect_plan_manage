package com.College.timetable.Util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;

import com.College.timetable.Repository.*;

@Configuration
public class DataInitializer {

    @Value("${app.admin.email:#{null}}")
    private String adminEmail;

    @Value("${app.admin.password:#{null}}")
    private String adminPassword;

    @Value("${app.admin.name:Admin User}")
    private String adminName;

    @Value("${app.admin.employee-id:EMP00001}")
    private String adminEmployeeId;

    @Autowired
    private DataSeederService dataSeederService;

    @Bean
    CommandLineRunner initDatabase(
            Teacher_Repo teacherRepo,
            Dep_repo depRepo,
            AcademicYearRepository academicYearRepo,
            TimeSlot_repo timeSlotRepo,
            Room_repo roomRepo,
            Course_repo courseRepo,
            Division_repo divisionRepo,
            Batch_repo batchRepo,
            TimetableEntry_repo timetableEntryRepo,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            dataSeederService.seedDatabase(
                teacherRepo,
                depRepo,
                academicYearRepo,
                timeSlotRepo,
                roomRepo,
                courseRepo,
                divisionRepo,
                batchRepo,
                timetableEntryRepo,
                passwordEncoder,
                adminEmail,
                adminPassword,
                adminName,
                adminEmployeeId
            );
        };
    }
}


