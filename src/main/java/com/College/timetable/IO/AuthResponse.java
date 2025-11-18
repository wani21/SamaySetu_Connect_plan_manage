package com.College.timetable.IO;



import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
	
	private String email;
	private String token;
	
	private String role;
}
