package com.College.timetable.IO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminStaffUpdateRequest {
    
    @NotBlank(message = "Name is required")
    @Size(max = 100)
    private String name;
    
    @NotBlank(message = "Employee ID is required")
    @Size(max = 20)
    private String employeeId;
    
    @NotBlank(message = "Short name is required")
    @Size(min = 2, max = 5, message = "Short name must be 2-5 characters")
    @jakarta.validation.constraints.Pattern(regexp = "^[A-Z]{2,5}$", message = "Short name must contain only uppercase letters (A-Z)")
    private String shortName;
    
    @Email(message = "Invalid email format")
    private String email;
    
    @Size(max = 15)
    private String phone;
    
    private String specialization;
    
    @Min(value = 1, message = "Minimum weekly hours must be at least 1")
    @Max(value = 40, message = "Minimum weekly hours cannot exceed 40")
    private Integer minWeeklyHours;
    
    @Min(value = 1, message = "Maximum weekly hours must be at least 1")
    @Max(value = 50, message = "Maximum weekly hours cannot exceed 50")
    private Integer maxWeeklyHours;
    
    private Long departmentId;
    
    private Boolean isActive;
}
