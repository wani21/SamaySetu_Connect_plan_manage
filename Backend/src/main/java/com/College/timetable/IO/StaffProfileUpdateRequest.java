package com.College.timetable.IO;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StaffProfileUpdateRequest {

    @Size(max = 15)
    private String phone;

    private String specialization;

    @jakarta.validation.constraints.Min(value = 1, message = "Min weekly hours must be at least 1")
    @jakarta.validation.constraints.Max(value = 40, message = "Min weekly hours cannot exceed 40")
    private Integer minWeeklyHours;

    @jakarta.validation.constraints.Min(value = 1, message = "Max weekly hours must be at least 1")
    @jakarta.validation.constraints.Max(value = 50, message = "Max weekly hours cannot exceed 50")
    private Integer maxWeeklyHours;
}