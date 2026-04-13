package com.College.timetable.IO;

import com.College.timetable.Entity.DayOfWeek;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AvailabilityDTO {

    @NotNull(message = "Day of week is required")
    private DayOfWeek dayOfWeek;

    @NotNull(message = "Start time is required")
    private String startTime;

    @NotNull(message = "End time is required")
    private String endTime;

    private Boolean isAvailable = true;
}
