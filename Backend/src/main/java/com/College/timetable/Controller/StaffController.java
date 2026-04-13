package com.College.timetable.Controller;

import java.security.Principal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.College.timetable.Entity.DayOfWeek;
import com.College.timetable.Entity.TeacherAvailability;
import com.College.timetable.Entity.TeacherEntity;
import com.College.timetable.IO.ChangePasswordRequest;
import com.College.timetable.IO.StaffProfileUpdateRequest;
import com.College.timetable.Repository.TeacherAvailability_repo;
import com.College.timetable.Service.TeacherService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/staff")
@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'HOD', 'TIMETABLE_COORDINATOR')")
public class StaffController {

    @Autowired
    private TeacherService teacherService;

    @Autowired
    private TeacherAvailability_repo availabilityRepo;
    
    @GetMapping("/profile")
    public ResponseEntity<TeacherEntity> getProfile(Principal principal) {
        try {
            TeacherEntity teacher = teacherService.getByEmail(principal.getName());
            return ResponseEntity.ok(teacher);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PutMapping("/profile")
    public ResponseEntity<TeacherEntity> updateProfile(
            @Valid @RequestBody StaffProfileUpdateRequest request, 
            Principal principal) {
        try {
            TeacherEntity updated = teacherService.updateStaffProfile(principal.getName(), request);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Principal principal) {
        try {
            // Validate password confirmation
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                return ResponseEntity.badRequest().body("New passwords do not match");
            }
            
            teacherService.changePassword(
                principal.getName(), 
                request.getCurrentPassword(), 
                request.getNewPassword()
            );
            
            return ResponseEntity.ok("Password changed successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── Availability Endpoints ──

    @GetMapping("/availability")
    public ResponseEntity<List<TeacherAvailability>> getAvailability(Principal principal) {
        TeacherEntity teacher = teacherService.getByEmail(principal.getName());
        return ResponseEntity.ok(availabilityRepo.findByTeacherId(teacher.getId()));
    }

    /**
     * Save teacher availability — replaces ALL existing entries for this teacher.
     * Uses typed DTO with validation instead of raw Map.
     */
    @jakarta.transaction.Transactional
    @PutMapping("/availability")
    public ResponseEntity<String> saveAvailability(
            @Valid @RequestBody List<com.College.timetable.IO.AvailabilityDTO> entries,
            Principal principal) {
        try {
            TeacherEntity teacher = teacherService.getByEmail(principal.getName());

            // Delete existing and replace atomically within transaction
            availabilityRepo.deleteByTeacherId(teacher.getId());

            List<TeacherAvailability> newEntries = new ArrayList<>();
            for (var dto : entries) {
                TeacherAvailability avail = new TeacherAvailability();
                avail.setTeacher(teacher);
                avail.setDayOfWeek(dto.getDayOfWeek());
                avail.setStartTime(LocalTime.parse(dto.getStartTime()));
                avail.setEndTime(LocalTime.parse(dto.getEndTime()));
                avail.setIsAvailable(dto.getIsAvailable());
                newEntries.add(avail);
            }
            availabilityRepo.saveAll(newEntries);

            return ResponseEntity.ok("Availability saved successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to save availability: " + e.getMessage());
        }
    }
}