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

	
	public TeacherEntity add(TeacherEntity teach) {
		// Validate department exists
		if (teach.getDepartment() != null && teach.getDepartment().getId() != null) {
			DepartmentEntity depart = department.findById(teach.getDepartment().getId())
				.orElseThrow(() -> new EntityNotFoundException("Department not found"));
		}
		teach.setPassword(passEncode.encode(teach.getPassword()));
        return teacher.save(teach);
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
		newTeacher.setIsActive(false); // Inactive until email verified and admin approved
		newTeacher.setIsEmailVerified(false);
		newTeacher.setIsApproved(false); // Requires admin approval after email verification
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
		
		// Verify email - but keep inactive until admin approves
		teacherEntity.setIsEmailVerified(true);
		teacherEntity.setIsActive(false); // Still inactive - waiting for admin approval
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
		
		// Check if approved by admin
		if (!teach.getIsApproved()) {
			throw new RuntimeException("Your account is pending admin approval. Please wait for approval.");
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
	
	public java.util.List<TeacherEntity> getAll() {
		return teacher.findAll();
	}
	
	public TeacherEntity getById(Long id) {
		return teacher.findById(id)
			.orElseThrow(() -> new EntityNotFoundException("Teacher not found with id: " + id));
	}
	
	public TeacherEntity getByEmail(String email) {
		return teacher.findByEmail(email)
			.orElseThrow(() -> new EntityNotFoundException("Teacher not found with email: " + email));
	}
	
	public TeacherEntity updateProfile(Long id, com.College.timetable.IO.ProfileUpdateRequest request) {
		TeacherEntity existing = getById(id);
		existing.setName(request.getName());
		existing.setEmployeeId(request.getEmployeeId());
		existing.setEmail(request.getEmail());
		existing.setPhone(request.getPhone());
		existing.setWeeklyHoursLimit(request.getWeeklyHoursLimit());
		existing.setSpecialization(request.getSpecialization());
		
		// Update password only if provided
		if (request.getPassword() != null && !request.getPassword().isEmpty()) {
			existing.setPassword(passEncode.encode(request.getPassword()));
		}
		
		// Update department if provided
		if (request.getDepartmentId() != null) {
			DepartmentEntity depart = department.findById(request.getDepartmentId())
				.orElseThrow(() -> new EntityNotFoundException("Department not found"));
			existing.setDepartment(depart);
		}
		
		return teacher.save(existing);
	}
	
	public TeacherEntity update(Long id, TeacherEntity teach) {
		TeacherEntity existing = getById(id);
		existing.setName(teach.getName());
		existing.setEmployeeId(teach.getEmployeeId());
		existing.setEmail(teach.getEmail());
		existing.setPhone(teach.getPhone());
		existing.setWeeklyHoursLimit(teach.getWeeklyHoursLimit());
		existing.setSpecialization(teach.getSpecialization());
		existing.setIsActive(teach.getIsActive());
		
		// Update password only if provided
		if (teach.getPassword() != null && !teach.getPassword().isEmpty()) {
			existing.setPassword(passEncode.encode(teach.getPassword()));
		}
		
		// Update department if provided
		if (teach.getDepartment() != null && teach.getDepartment().getId() != null) {
			DepartmentEntity depart = department.findById(teach.getDepartment().getId())
				.orElseThrow(() -> new EntityNotFoundException("Department not found"));
			existing.setDepartment(depart);
		}
		
		return teacher.save(existing);
	}
	
	public void delete(Long id) {
		if (!teacher.existsById(id)) {
			throw new EntityNotFoundException("Teacher not found with id: " + id);
		}
		teacher.deleteById(id);
	}
	
	// Admin approval methods
	public java.util.List<TeacherEntity> getPendingApprovals() {
		return teacher.findByIsApprovedAndIsEmailVerified(false, true);
	}
	
	public TeacherEntity approveTeacher(Long id) {
		TeacherEntity teacherEntity = getById(id);
		
		if (!teacherEntity.getIsEmailVerified()) {
			throw new RuntimeException("Cannot approve teacher - email not verified");
		}
		
		teacherEntity.setIsApproved(true);
		teacherEntity.setIsActive(true);
		
		TeacherEntity approved = teacher.save(teacherEntity);
		
		// Send approval notification email
		try {
			emailService.sendApprovalEmail(teacherEntity.getEmail(), teacherEntity.getName());
		} catch (Exception e) {
			System.err.println("Failed to send approval email: " + e.getMessage());
		}
		
		return approved;
	}
	
	public TeacherEntity rejectTeacher(Long id, String reason) {
		TeacherEntity teacherEntity = getById(id);
		
		teacherEntity.setIsApproved(false);
		teacherEntity.setIsActive(false);
		
		TeacherEntity rejected = teacher.save(teacherEntity);
		
		// Send rejection notification email
		try {
			emailService.sendRejectionEmail(teacherEntity.getEmail(), teacherEntity.getName(), reason);
		} catch (Exception e) {
			System.err.println("Failed to send rejection email: " + e.getMessage());
		}
		
		return rejected;
	}
	
}
