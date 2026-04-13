package com.College.timetable.IO;


import com.College.timetable.Entity.DayOfWeek;
import com.College.timetable.Entity.Semester;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO for creating/updating a single timetable entry (manual mode)
 */
@Data
public class CreateTimetableEntryDTO {

    @NotNull(message = "Division is required")
    private Long divisionId;

    @NotNull(message = "Course is required")
    private Long courseId;

    @NotNull(message = "Teacher is required")
    private Long teacherId;

    @NotNull(message = "Room is required")
    private Long roomId;

    @NotNull(message = "Time slot is required")
    private Long timeSlotId;

    @NotNull(message = "Day of week is required")
    private DayOfWeek dayOfWeek;

    @NotNull(message = "Academic year is required")
    private Long academicYearId;

    private Semester semester;

    private String notes;

    // For lab entries — both must be provided together
    private Long labSessionGroupId;  // null = theory class
    private Long batchId;            // null = theory class
}
