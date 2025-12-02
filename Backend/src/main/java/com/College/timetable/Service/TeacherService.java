package com.College.timetable.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import org.springframework.context.annotation.Lazy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.College.timetable.Entity.DepartmentEntity;
import com.College.timetable.Entity.TeacherEntity;
import com.College.timetable.Repository.Dep_repo;
import com.College.timetable.Repository.Teacher_Repo;

import jakarta.persistence.EntityNotFoundException;

@Service
public class TeacherService implements UserDetailsService{
	
	@Autowired
	private Teacher_Repo teacher;
	
	@Autowired
	private Dep_repo department;
	
	@Autowired
	@Lazy
    PasswordEncoder passEncode;
    
    @Autowired
    private EmailService emailService;

	
	public void add(TeacherEntity teach) {
		// Validate department exists
		if (teach.getDepartment() != null && teach.getDepartment().getId() != null) {
			DepartmentEntity depart = department.findById(teach.getDepartment().getId())
				.orElseThrow(() -> new EntityNotFoundException("Department not found"));
		}
		teach.setPassword(passEncode.encode(teach.getPassword()));
        teacher.save(teach);
	}
	
	public void register(com.College.timetable.IO.RegisterRequest request) {
		// Validate college email
		if (!request.getEmail().endsWith("@mitaoe.ac.in")) {
			throw new RuntimeException("Only college email (@mitaoe.ac.in) is allowed");
		}
		
		// Check if email already exists
		if (teacher.findByEmail(request.getEmail()).isPresent()) {
			throw new RuntimeException("Email already registered");
		}
		
		// Check if employee ID already exists
		if (teacher.findByEmployeeId(request.getEmployeeId()).isPresent()) {
			throw new RuntimeException("Employee ID already exists");
		}
		
		// Generate verification token
		String verificationToken = UUID.randomUUID().toString();
		
		TeacherEntity newTeacher = new TeacherEntity();
		newTeacher.setName(request.getName());
		newTeacher.setEmployeeId(request.getEmployeeId());
		newTeacher.setEmail(request.getEmail());
		newTeacher.setPhone(request.getPhone());
		newTeacher.setSpecialization(request.getSpecialization());
		newTeacher.setPassword(passEncode.encode(request.getPassword()));
		newTeacher.setRole("TEACHER");
		newTeacher.setIsActive(false); // Inactive until email verified
		newTeacher.setIsEmailVerified(false);
		newTeacher.setVerificationToken(verificationToken);
		newTeacher.setVerificationTokenExpiry(Timestamp.valueOf(LocalDateTime.now().plusHours(24)));
		newTeacher.setWeeklyHoursLimit(25);
		
		// Set department if provided
		if (request.getDepartmentId() != null) {
			DepartmentEntity dept = department.findById(request.getDepartmentId())
				.orElseThrow(() -> new EntityNotFoundException("Department not found"));
			newTeacher.setDepartment(dept);
		}
		
		teacher.save(newTeacher);
		
		// Send verification email
		try {
			emailService.sendVerificationEmail(request.getEmail(), verificationToken);
		} catch (Exception e) {
			// Log error but don't fail registration
			System.err.println("Failed to send verification email: " + e.getMessage());
		}
	}
	
	public void verifyEmail(String token) {
		TeacherEntity teacherEntity = teacher.findByVerificationToken(token)
			.orElseThrow(() -> new RuntimeException("Invalid verification token"));
		
		// Check if token expired
		if (teacherEntity.getVerificationTokenExpiry().before(new Timestamp(System.currentTimeMillis()))) {
			throw new RuntimeException("Verification token has expired");
		}
		
		// Verify email
		teacherEntity.setIsEmailVerified(true);
		teacherEntity.setIsActive(true);
		teacherEntity.setVerificationToken(null);
		teacherEntity.setVerificationTokenExpiry(null);
		
		teacher.save(teacherEntity);
		
		// Send welcome email
		try {
			emailService.sendWelcomeEmail(teacherEntity.getEmail(), teacherEntity.getName());
		} catch (Exception e) {
			System.err.println("Failed to send welcome email: " + e.getMessage());
		}
	}
	
	public void forgotPassword(String email) {
		TeacherEntity teacherEntity = teacher.findByEmail(email)
			.orElseThrow(() -> new RuntimeException("Email not found"));
		
		// Generate password reset token
		String resetToken = UUID.randomUUID().toString();
		
		teacherEntity.setPasswordResetToken(resetToken);
		teacherEntity.setPasswordResetTokenExpiry(Timestamp.valueOf(LocalDateTime.now().plusHours(1)));
		
		teacher.save(teacherEntity);
		
		// Send password reset email
		try {
			emailService.sendPasswordResetEmail(email, resetToken);
		} catch (Exception e) {
			throw new RuntimeException("Failed to send password reset email");
		}
	}
	
	public void resetPassword(String token, String newPassword) {
		TeacherEntity teacherEntity = teacher.findByPasswordResetToken(token)
			.orElseThrow(() -> new RuntimeException("Invalid password reset token"));
		
		// Check if token expired
		if (teacherEntity.getPasswordResetTokenExpiry().before(new Timestamp(System.currentTimeMillis()))) {
			throw new RuntimeException("Password reset token has expired");
		}
		
		// Reset password
		teacherEntity.setPassword(passEncode.encode(newPassword));
		teacherEntity.setPasswordResetToken(null);
		teacherEntity.setPasswordResetTokenExpiry(null);
		
		teacher.save(teacherEntity);
	}
	
	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		
		TeacherEntity teach=teacher.findByEmail(email).orElseThrow(()->new RuntimeException("The user is not found"));
		
		// Check if email is verified
		if (!teach.getIsEmailVerified()) {
			throw new RuntimeException("Email not verified. Please check your email for verification link.");
		}
		
		// Check if account is active
		if (!teach.getIsActive()) {
			throw new RuntimeException("Account is not active. Please contact administrator.");
		}
		
		return new User(
				teach.getEmail(),
				teach.getPassword(),
				Collections.singleton(new SimpleGrantedAuthority("ROLE_" + teach.getRole()))
				);
		
	}
	
	public String getByRole(String email) {
		TeacherEntity teach = teacher.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return teach.getRole(); 
    }

	
}
