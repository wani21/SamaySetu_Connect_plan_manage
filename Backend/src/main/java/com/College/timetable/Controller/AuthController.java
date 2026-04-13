package com.College.timetable.Controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.College.timetable.IO.AuthRequest;
import com.College.timetable.IO.AuthResponse;
import com.College.timetable.IO.ChangeFirstPasswordRequest;
import com.College.timetable.IO.ForgotPasswordRequest;
import com.College.timetable.IO.RegisterRequest;
import com.College.timetable.IO.ResetPasswordRequest;
import com.College.timetable.Service.TeacherService;
import com.College.timetable.Util.JWTUtil;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/auth")
public class AuthController {

	private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

	@Autowired
	private PasswordEncoder passwordEncode;

	@Autowired
	private AuthenticationManager authManager;

	@Autowired
	private JWTUtil jwtutil;

	@Autowired
	private TeacherService teacherservice;

	@Value("${app.frontend.url:http://localhost:5173}")
	private String frontendUrl;
	
	//handler method
	@PostMapping("/")
	public String encodePassword(@RequestBody Map<String,String> request) {
		return passwordEncode.encode(request.get("password"));
	}
	
	@PostMapping("/register")
	public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
		try {
			teacherservice.register(request);
			return ResponseEntity.ok("Registration successful! Please check your college email to verify your account.");
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}
	
	@GetMapping("/verify-email")
	public ResponseEntity<String> verifyEmail(@RequestParam("token") String token) {
		try {
			teacherservice.verifyEmail(token);
			return ResponseEntity.ok("Email verified successfully! You can now login.");
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}
	
	@PostMapping("/forgot-password")
	public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
		// Always return the same message to prevent email enumeration attacks.
		// An attacker cannot determine whether an email exists in the system.
		try {
			teacherservice.forgotPassword(request.getEmail());
			logger.info("[AUTH] Password reset requested for email: {}", request.getEmail());
		} catch (Exception e) {
			// Log the failure server-side but do NOT expose it to the client
			logger.warn("[AUTH] Password reset failed for email: {} - {}", request.getEmail(), e.getMessage());
		}
		return ResponseEntity.ok("If this email is registered, you will receive a password reset link.");
	}
	
	@PostMapping("/reset-password")
	public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
		try {
			teacherservice.resetPassword(request.getToken(), request.getNewPassword());
			return ResponseEntity.ok("Password reset successfully! You can now login with your new password.");
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}
	
	@GetMapping("/reset-password")
	public ResponseEntity<String> resetPasswordRedirect(@RequestParam("token") String token) {
		try {
			teacherservice.validateResetToken(token);
			String encodedToken = java.net.URLEncoder.encode(token, java.nio.charset.StandardCharsets.UTF_8);
			return ResponseEntity.status(HttpStatus.FOUND)
					.header("Location", frontendUrl + "/reset-password?token=" + encodedToken)
					.body("Redirecting to password reset page...");
		} catch (Exception e) {
			String encodedError = java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
			return ResponseEntity.status(HttpStatus.FOUND)
					.header("Location", frontendUrl + "/reset-password?error=" + encodedError)
					.body("Redirecting to password reset page...");
		}
	}
	
	@PostMapping("/login")
	public AuthResponse login(@Valid @RequestBody AuthRequest request) {
		logger.info("[LOGIN] Login attempt for: {}", request.getEmail());
		try {
			// Step 0: Check if account is locked
			teacherservice.checkAccountLocked(request.getEmail());

			// Step 1: Check user status (email verification, approval, etc.)
			final UserDetails user = teacherservice.loadUserByUsername(request.getEmail());

			// Step 2: Authenticate credentials (BCrypt check)
			try {
				authenticate(request.getEmail(), request.getPassword());
			} catch (Exception authEx) {
				// Password wrong — record failure and possibly lock
				teacherservice.recordFailedLogin(request.getEmail());
				throw authEx;
			}

			// Step 3: Password correct — reset any failed attempts
			teacherservice.resetFailedLogins(request.getEmail());

			// Step 4: Get role and check first login
			String role = teacherservice.getByRole(request.getEmail());
			boolean isFirstLogin = !"ADMIN".equals(role) && teacherservice.isFirstLogin(request.getEmail());

			// Step 5: Generate JWT
			final String token = jwtutil.generateToken(user);

			AuthResponse response = new AuthResponse(request.getEmail(), token, role);
			response.setFirstLogin(isFirstLogin);

			logger.info("[LOGIN] Success: {} (role={})", request.getEmail(), role);
			return response;
		} catch (RuntimeException e) {
			logger.warn("[LOGIN] Failed for {}: {}", request.getEmail(), e.getMessage());
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
		} catch (Exception e) {
			logger.error("[LOGIN] Unexpected error for {}", request.getEmail(), e);
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred. Please try again later.");
		}
	}
	
	@PostMapping("/change-first-password")
	public ResponseEntity<String> changeFirstPassword(@Valid @RequestBody ChangeFirstPasswordRequest request) {
		try {
			teacherservice.updateFirstLoginPassword(request.getEmail(), request.getNewPassword());
			return ResponseEntity.ok("Password updated successfully! Please login with your new password.");
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}
	
	private void authenticate(String email, String password) {
		try {
			logger.info("[LOGIN] authenticate() calling AuthenticationManager for {}", email);
			authManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
			logger.info("[LOGIN] authenticate() AuthenticationManager accepted credentials for {}", email);
		} catch (Exception e) {
			// Log full cause for debugging
			logger.warn("[LOGIN] authenticate() FAILED for {} - Cause: {} - Msg: {}",
					email, e.getClass().getSimpleName(), e.getMessage());
			// Use generic message to prevent email enumeration attacks
			throw new RuntimeException("Invalid email or password");
		}
	}

}
