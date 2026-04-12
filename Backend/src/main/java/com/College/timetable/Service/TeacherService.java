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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.College.timetable.Entity.DepartmentEntity;
import com.College.timetable.Entity.TeacherEntity;
import com.College.timetable.Repository.Dep_repo;
import com.College.timetable.Repository.Teacher_Repo;

import jakarta.persistence.EntityNotFoundException;

@Service
public class TeacherService implements UserDetailsService{

	private static final Logger logger = LoggerFactory.getLogger(TeacherService.class);
	
	@Autowired
	private Teacher_Repo teacher;

	@Autowired
	private Dep_repo department;

	@Autowired
	private AuditLogService auditLog;
	
	@Autowired
	@Lazy
    PasswordEncoder passEncode;
    
    @Autowired
    private EmailService emailService;

	
	@Transactional
	public TeacherEntity add(TeacherEntity teach) {
		// Validate department exists
		if (teach.getDepartment() != null && teach.getDepartment().getId() != null) {
			DepartmentEntity depart = department.findById(teach.getDepartment().getId())
				.orElseThrow(() -> new EntityNotFoundException("Department not found"));
		}
		teach.setPassword(passEncode.encode(teach.getPassword()));
        return teacher.save(teach);
	}

	@Transactional
	public void register(com.College.timetable.IO.RegisterRequest request) {
		// Validate college email
		if (!request.getEmail().endsWith("@mitaoe.ac.in")) {
			throw new RuntimeException("Only college email (@mitaoe.ac.in) is allowed");
		}
		
		// Check if email already exists — generic message to prevent enumeration
		if (teacher.findByEmail(request.getEmail()).isPresent()) {
			throw new RuntimeException("Registration could not be completed. Please contact the administrator.");
		}

		// Check if employee ID already exists
		if (teacher.findByEmployeeId(request.getEmployeeId()).isPresent()) {
			throw new RuntimeException("Registration could not be completed. Please contact the administrator.");
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
		newTeacher.setIsFirstLogin(false); // Self-registered users don't need to change password
		newTeacher.setVerificationToken(verificationToken);
		newTeacher.setVerificationTokenExpiry(Timestamp.valueOf(LocalDateTime.now().plusHours(24)));
		newTeacher.setMinWeeklyHours(10);
		newTeacher.setMaxWeeklyHours(30);
		
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
			logger.error("Failed to send verification email: {}", e.getMessage());
		}
	}
	
	@Transactional
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
			logger.error("Failed to send welcome email: {}", e.getMessage());
		}
	}

	@Transactional
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
	
	@Transactional
	public void resetPassword(String token, String newPassword) {
		TeacherEntity teacherEntity = teacher.findByPasswordResetToken(token)
			.orElseThrow(() -> new RuntimeException("Invalid or expired reset link. Please request a new password reset."));

		// Check if token expired
		if (teacherEntity.getPasswordResetTokenExpiry().before(new Timestamp(System.currentTimeMillis()))) {
			throw new RuntimeException("Reset link has expired. Please request a new password reset.");
		}

		// Update password and clear reset token
		teacherEntity.setPassword(passEncode.encode(newPassword));
		teacherEntity.setPasswordResetToken(null);
		teacherEntity.setPasswordResetTokenExpiry(null);

		teacher.save(teacherEntity);
	}

	@Transactional(readOnly = true)
	public void validateResetToken(String token) {
		TeacherEntity teacherEntity = teacher.findByPasswordResetToken(token)
			.orElseThrow(() -> new RuntimeException("Invalid or expired reset link. Please request a new password reset."));

		// Check if token expired
		if (teacherEntity.getPasswordResetTokenExpiry().before(new Timestamp(System.currentTimeMillis()))) {
			throw new RuntimeException("Reset link has expired. Please request a new password reset.");
		}
	}

	@Transactional(readOnly = true)
	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		logger.info("[LOGIN] loadUserByUsername START for email: {}", email);

		TeacherEntity teach = teacher.findByEmail(email).orElseThrow(() -> {
			logger.warn("[LOGIN] User NOT FOUND in database for email: {}", email);
			return new RuntimeException("The user is not found");
		});

		logger.info("[LOGIN] User found: id={}, role={}, isEmailVerified={}, isApproved={}, isActive={}, isFirstLogin={}",
				teach.getId(), teach.getRole(),
				teach.getIsEmailVerified(), teach.getIsApproved(),
				teach.getIsActive(), teach.getIsFirstLogin());
		// SECURITY: Never log password hashes — confirmed password is set
		logger.debug("[LOGIN] Password present: {}", teach.getPassword() != null);

		// Check if email is verified (null-safe)
		if (teach.getIsEmailVerified() == null || !teach.getIsEmailVerified()) {
			logger.warn("[LOGIN] Email not verified for {}", email);
			throw new RuntimeException("Email not verified. Please check your email for verification link.");
		}

		// Check if approved by admin (null-safe)
		if (teach.getIsApproved() == null || !teach.getIsApproved()) {
			logger.warn("[LOGIN] Account not approved for {}", email);
			throw new RuntimeException("Your account is pending admin approval. Please wait for approval.");
		}

		// Check if account is active (null-safe)
		if (teach.getIsActive() == null || !teach.getIsActive()) {
			logger.warn("[LOGIN] Account not active for {}", email);
			throw new RuntimeException("Account is not active. Please contact administrator.");
		}

		// Normalize role to UPPERCASE so Spring's hasRole("ADMIN") matches
		// regardless of how the role was stored in the DB (admin, Admin, ADMIN, etc.)
		String roleRaw = teach.getRole();
		if (roleRaw == null || roleRaw.isBlank()) {
			logger.error("[LOGIN] User {} has NULL/blank role in DB - cannot grant any authority", email);
			throw new RuntimeException("User has no role assigned. Contact administrator.");
		}
		String normalizedRole = "ROLE_" + roleRaw.trim().toUpperCase();
		logger.info("[LOGIN] All checks passed, granting authority [{}] for {}", normalizedRole, email);
		return new User(
				teach.getEmail(),
				teach.getPassword(),
				Collections.singleton(new SimpleGrantedAuthority(normalizedRole))
				);
	}

	@Transactional(readOnly = true)
	public boolean isFirstLogin(String email) {
		logger.info("[LOGIN] isFirstLogin check for {}", email);
		TeacherEntity teach = teacher.findByEmail(email)
			.orElseThrow(() -> new RuntimeException("User not found"));
		boolean result = teach.getIsFirstLogin() != null ? teach.getIsFirstLogin() : false;
		logger.info("[LOGIN] isFirstLogin result for {} = {}", email, result);
		return result;
	}

	@Transactional
	public void updateFirstLoginPassword(String email, String newPassword) {
		TeacherEntity teach = teacher.findByEmail(email)
			.orElseThrow(() -> new RuntimeException("User not found"));

		// Handle null values for existing users
		Boolean isFirstLogin = teach.getIsFirstLogin();
		if (isFirstLogin == null || !isFirstLogin) {
			throw new RuntimeException("Password change not required");
		}

		teach.setPassword(passEncode.encode(newPassword));
		teach.setIsFirstLogin(false);
		teacher.save(teach);
	}

	// ── Account Lockout ──

	private static final int MAX_FAILED_ATTEMPTS = 5;
	private static final int LOCKOUT_MINUTES = 15;

	/**
	 * Check if account is currently locked. Throws RuntimeException if locked.
	 */
	@Transactional(readOnly = true)
	public void checkAccountLocked(String email) {
		teacher.findByEmail(email).ifPresent(t -> {
			if (t.getAccountLockedUntil() != null && t.getAccountLockedUntil().after(new java.sql.Timestamp(System.currentTimeMillis()))) {
				long minutesLeft = (t.getAccountLockedUntil().getTime() - System.currentTimeMillis()) / 60000 + 1;
				throw new RuntimeException("Account temporarily locked due to too many failed login attempts. Try again in " + minutesLeft + " minutes.");
			}
		});
	}

	/**
	 * Record a failed login attempt. Locks account after MAX_FAILED_ATTEMPTS.
	 */
	@Transactional
	public void recordFailedLogin(String email) {
		teacher.findByEmail(email).ifPresent(t -> {
			int attempts = (t.getFailedLoginAttempts() != null ? t.getFailedLoginAttempts() : 0) + 1;
			t.setFailedLoginAttempts(attempts);
			if (attempts >= MAX_FAILED_ATTEMPTS) {
				t.setAccountLockedUntil(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES)));
				logger.warn("[SECURITY] Account locked for {} after {} failed attempts", email, attempts);
			}
			teacher.save(t);
		});
	}

	/**
	 * Reset failed login counter on successful authentication.
	 */
	@Transactional
	public void resetFailedLogins(String email) {
		teacher.findByEmail(email).ifPresent(t -> {
			if (t.getFailedLoginAttempts() != null && t.getFailedLoginAttempts() > 0) {
				t.setFailedLoginAttempts(0);
				t.setAccountLockedUntil(null);
				teacher.save(t);
			}
		});
	}

	@Transactional(readOnly = true)
	public String getByRole(String email) {
		logger.info("[LOGIN] getByRole for {}", email);
		TeacherEntity teach = teacher.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found"));
		logger.info("[LOGIN] getByRole result for {} = {}", email, teach.getRole());
        return teach.getRole();
    }

	@Transactional(readOnly = true)
	public java.util.List<TeacherEntity> getAll() {
		return teacher.findAll();
	}

	@Transactional(readOnly = true)
	public TeacherEntity getById(Long id) {
		return teacher.findById(id)
			.orElseThrow(() -> new EntityNotFoundException("Teacher not found with id: " + id));
	}

	@Transactional(readOnly = true)
	public TeacherEntity getByEmail(String email) {
		return teacher.findByEmail(email)
			.orElseThrow(() -> new EntityNotFoundException("Teacher not found with email: " + email));
	}

	@Transactional
	public TeacherEntity updateProfile(Long id, com.College.timetable.IO.ProfileUpdateRequest request) {
		TeacherEntity existing = getById(id);
		existing.setName(request.getName());
		existing.setEmployeeId(request.getEmployeeId());
		existing.setEmail(request.getEmail());
		existing.setPhone(request.getPhone());
		existing.setMinWeeklyHours(request.getMinWeeklyHours());
		existing.setMaxWeeklyHours(request.getMaxWeeklyHours());
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
	
	@Transactional
	public TeacherEntity updateStaffProfile(String email, com.College.timetable.IO.StaffProfileUpdateRequest request) {
		TeacherEntity existing = getByEmail(email);

		// Only allow editing of specific fields for staff (name is NOT editable)
		if (request.getPhone() != null) existing.setPhone(request.getPhone());
		if (request.getSpecialization() != null) existing.setSpecialization(request.getSpecialization());

		// Weekly hours — validate min < max
		if (request.getMinWeeklyHours() != null) existing.setMinWeeklyHours(request.getMinWeeklyHours());
		if (request.getMaxWeeklyHours() != null) existing.setMaxWeeklyHours(request.getMaxWeeklyHours());

		if (existing.getMinWeeklyHours() != null && existing.getMaxWeeklyHours() != null
				&& existing.getMinWeeklyHours() > existing.getMaxWeeklyHours()) {
			throw new RuntimeException("Minimum weekly hours cannot exceed maximum weekly hours");
		}

		return teacher.save(existing);
	}

	@Transactional
	public void changePassword(String email, String currentPassword, String newPassword) {
		TeacherEntity existing = getByEmail(email);

		// Verify current password
		if (!passEncode.matches(currentPassword, existing.getPassword())) {
			throw new RuntimeException("Current password is incorrect");
		}

		// Validate new password
		if (newPassword == null || newPassword.length() < 6) {
			throw new RuntimeException("New password must be at least 6 characters");
		}

		// Don't allow same password
		if (passEncode.matches(newPassword, existing.getPassword())) {
			throw new RuntimeException("New password must be different from current password");
		}

		// Update password
		existing.setPassword(passEncode.encode(newPassword));
		teacher.save(existing);
	}

	@Transactional
	public TeacherEntity update(Long id, TeacherEntity teach) {
		TeacherEntity existing = getById(id);
		existing.setName(teach.getName());
		existing.setEmployeeId(teach.getEmployeeId());
		existing.setEmail(teach.getEmail());
		existing.setPhone(teach.getPhone());
		existing.setMinWeeklyHours(teach.getMinWeeklyHours());
		existing.setMaxWeeklyHours(teach.getMaxWeeklyHours());
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

	@Transactional
	public void delete(Long id) {
		if (!teacher.existsById(id)) {
			throw new EntityNotFoundException("Teacher not found with id: " + id);
		}
		teacher.deleteById(id);
	}

	// Admin approval methods
	@Transactional(readOnly = true)
	public java.util.List<TeacherEntity> getPendingApprovals() {
		return teacher.findByIsApprovedAndIsEmailVerified(false, true);
	}

	@Transactional
	public TeacherEntity approveTeacher(Long id) {
		TeacherEntity teacherEntity = getById(id);

		if (!teacherEntity.getIsEmailVerified()) {
			throw new RuntimeException("Cannot approve teacher - email not verified");
		}

		teacherEntity.setIsApproved(true);
		teacherEntity.setIsActive(true);

		TeacherEntity approved = teacher.save(teacherEntity);
		auditLog.log("APPROVE_TEACHER", "teacherId=" + id + " email=" + teacherEntity.getEmail());

		// Send approval notification email
		try {
			emailService.sendApprovalEmail(teacherEntity.getEmail(), teacherEntity.getName());
		} catch (Exception e) {
			logger.error("Failed to send approval email: {}", e.getMessage());
		}

		return approved;
	}

	@Transactional
	public TeacherEntity rejectTeacher(Long id, String reason) {
		TeacherEntity teacherEntity = getById(id);

		teacherEntity.setIsApproved(false);
		teacherEntity.setIsActive(false);

		TeacherEntity rejected = teacher.save(teacherEntity);
		auditLog.log("REJECT_TEACHER", "teacherId=" + id + " email=" + teacherEntity.getEmail() + " reason=" + reason);

		// Send rejection notification email
		try {
			emailService.sendRejectionEmail(teacherEntity.getEmail(), teacherEntity.getName(), reason);
		} catch (Exception e) {
			logger.error("Failed to send rejection email: {}", e.getMessage());
		}

		return rejected;
	}
	
}
