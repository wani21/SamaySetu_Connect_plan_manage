package com.College.timetable.Controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
import com.College.timetable.Service.TeacherService;
import com.College.timetable.Util.JWTUtil;

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
