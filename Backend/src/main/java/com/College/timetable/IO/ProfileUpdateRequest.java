package com.College.timetable.IO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpdateRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "Employee ID is required")
    @Size(max = 20)
    private String employeeId;

    @Email(message = "Invalid email format")
    private String email;

    @Size(max = 15)
    private String phone;

    private Integer minWeeklyHours;

    private Integer maxWeeklyHours;

    private String specialization;

    // Password is optional - only include if changing password
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$", message = "Password must contain uppercase, lowercase, and a number")
    private String password;

    private Long departmentId;
}
