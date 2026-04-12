package com.College.timetable.IO;

import com.College.timetable.Entity.DayOfWeek;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class TimetableEntryRequest {

	 @NotNull(message = "Teacher ID is required")
	 private Long teacherId;

	 @NotNull(message = "Room ID is required")
	 private Long roomId;

	 @NotNull(message = "Division ID is required")
	 private Long divisionId;

	 @NotNull(message = "Time slot ID is required")
	 private Long timeSlotId;

	 @NotNull(message = "Academic year ID is required")
	 private Long academicYearId;

	 private Long labSessionGroupId;

	 @NotNull(message = "Day of week is required")
	 private DayOfWeek dayOfWeek;

	 @Min(value = 1, message = "Teacher max weekly hours must be at least 1")
	 private Integer teacherMaxWeeklyHours;

}
