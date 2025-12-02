package com.College.timetable.IO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    
    @NotBlank(message = "Name is required")
    @Size(max = 100)
    private String name;
    
    @NotBlank(message = "Employee ID is required")
    @Size(max = 20)
    private String employeeId;
    
    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    @Pattern(regexp = ".*@mitaoe\\.ac\\.in$", message = "Only college email (@mitaoe.ac.in) is allowed")
    private String email;
    
    @Size(max = 15)
    private String phone;
    
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
    
    private String specialization;
    
    private Long departmentId;
}
