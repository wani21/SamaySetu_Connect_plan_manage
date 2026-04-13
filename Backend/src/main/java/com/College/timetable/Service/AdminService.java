package com.College.timetable.Service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.College.timetable.Entity.DepartmentEntity;
import com.College.timetable.Entity.TeacherEntity;
import com.College.timetable.IO.AdminStaffUpdateRequest;
import com.College.timetable.Repository.Dep_repo;
import com.College.timetable.Repository.Teacher_Repo;

import jakarta.persistence.EntityNotFoundException;

@Service
public class AdminService {

    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);
    
    @Autowired
    private Teacher_Repo teacherRepository;
    
    @Autowired
    private Dep_repo departmentRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Transactional
    public int createStaffFromCSV(List<TeacherEntity> staffList) {
        int created = 0;

        for (TeacherEntity teacher : staffList) {
            try {
                // Check if employee already exists
                if (teacherRepository.findByEmployeeId(teacher.getEmployeeId()).isPresent()) {
                    continue; // Skip existing employees
                }

                if (teacherRepository.findByEmail(teacher.getEmail()).isPresent()) {
                    continue; // Skip existing emails
                }

                // Encode the default password
                teacher.setPassword(passwordEncoder.encode(teacher.getPassword()));

                // Save the teacher
                teacherRepository.save(teacher);
                created++;

            } catch (Exception e) {
                // Log error but continue with other records
                logger.warn("Error creating staff member {}: {}", teacher.getEmployeeId(), e.getMessage());
            }
        }

        return created;
    }

    @Transactional
    public TeacherEntity updateStaff(Long id, AdminStaffUpdateRequest request) {
        TeacherEntity existing = teacherRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Staff not found"));
        
        existing.setName(request.getName());
        existing.setEmployeeId(request.getEmployeeId());
        existing.setEmail(request.getEmail());
        existing.setPhone(request.getPhone());
        existing.setSpecialization(request.getSpecialization());
        existing.setMinWeeklyHours(request.getMinWeeklyHours());
        existing.setMaxWeeklyHours(request.getMaxWeeklyHours());
        
        if (request.getIsActive() != null) {
            existing.setIsActive(request.getIsActive());
        }
        
        // Update department if provided
        if (request.getDepartmentId() != null) {
            DepartmentEntity dept = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new EntityNotFoundException("Department not found"));
            existing.setDepartment(dept);
        } else {
            existing.setDepartment(null);
        }
        
        return teacherRepository.save(existing);
    }
}