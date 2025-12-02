package com.College.timetable.Controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
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
	
	@Autowired
	private PasswordEncoder passwordEncode;

	@Autowired
	private AuthenticationManager authManager;
	
	@Autowired
	private JWTUtil jwtutil;
	
	
	
	
	@Autowired
	private TeacherService teacherservice;
	
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
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
		}
	}
	
	@GetMapping("/verify-email")
	public ResponseEntity<String> verifyEmail(@RequestParam("token") String token) {
		try {
			teacherservice.verifyEmail(token);
			return ResponseEntity.ok("Email verified successfully! You can now login.");
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
		}
	}
	
	@PostMapping("/forgot-password")
	public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
		try {
			teacherservice.forgotPassword(request.getEmail());
			return ResponseEntity.ok("Password reset link has been sent to your email.");
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
		}
	}
	
	@PostMapping("/reset-password")
	public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
		try {
			teacherservice.resetPassword(request.getToken(), request.getNewPassword());
			return ResponseEntity.ok("Password reset successfully! You can now login with your new password.");
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
		}
	}
	
	@PostMapping("/login")
	public AuthResponse login(@RequestBody AuthRequest request) {
		authenticate(request.getEmail(),request.getPassword());
		final UserDetails user=teacherservice.loadUserByUsername(request.getEmail());
		final String token=jwtutil.generateToken(user);
		String role=teacherservice.getByRole(request.getEmail());
		return new AuthResponse(request.getEmail(),token,role);
	}
	
	private void authenticate(String email,String password) {
		try {
			authManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
		}
		catch(Exception e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email or password is incorrect");

		}
	}

}
