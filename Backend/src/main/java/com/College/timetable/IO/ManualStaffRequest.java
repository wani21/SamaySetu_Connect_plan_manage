package com.College.timetable.IO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManualStaffRequest {
    
    @NotBlank(message = "Name is required")
    private String name;
    
    @NotBlank(message = "Employee ID is required")
    private String employeeId;
    
    @NotBlank(message = "Short name is required")
    @jakarta.validation.constraints.Size(min = 2, max = 5, message = "Short name must be 2-5 characters")
    @jakarta.validation.constraints.Pattern(regexp = "^[A-Z]{2,5}$", message = "Short name must contain only uppercase letters (A-Z)")
    private String shortName;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    private String phone;
    
    private String specialization;
    
    @NotNull(message = "Minimum weekly hours is required")
    @Min(value = 1, message = "Minimum weekly hours must be at least 1")
    private Integer minWeeklyHours;
    
    @NotNull(message = "Maximum weekly hours is required")
    @Min(value = 1, message = "Maximum weekly hours must be at least 1")
    private Integer maxWeeklyHours;

    // Role: TEACHER (default), HOD, TIMETABLE_COORDINATOR
    // ADMIN role must be created separately via DataInitializer or direct DB
    private String role;

    // Department ID (optional — can be assigned later)
    private Long departmentId;
}
