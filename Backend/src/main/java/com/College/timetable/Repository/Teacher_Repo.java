package com.College.timetable.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.College.timetable.Entity.TeacherEntity;

@Repository
public interface Teacher_Repo extends JpaRepository<TeacherEntity, Long> {

    Optional<TeacherEntity> findByEmail(String email);
    
    Optional<TeacherEntity> findByEmployeeId(String employeeId);
    
    Optional<TeacherEntity> findByVerificationToken(String token);
    
    Optional<TeacherEntity> findByPasswordResetToken(String token);
    
    // Find teachers pending approval (email verified but not approved)
    java.util.List<TeacherEntity> findByIsApprovedAndIsEmailVerified(Boolean isApproved, Boolean isEmailVerified);

}