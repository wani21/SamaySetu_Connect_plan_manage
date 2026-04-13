package com.College.timetable.IO;


import com.College.timetable.Entity.DayOfWeek;
import com.College.timetable.Entity.Semester;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO for creating a lab session group.
 * Create this FIRST before adding individual batch entries.
 *
 * Flow:
 * 1. POST /api/timetable/lab-groups → creates group, returns groupId
 * 2. POST /api/timetable/entries (x3) with labSessionGroupId = groupId
 *    → one entry per batch (Batch A → Lab 1, Batch B → Lab 2, Batch C → Lab 3)
 */
@Data
public class CreateLabSessionGroupDTO {

    @NotNull private Long divisionId;
    @NotNull private Long courseId;
    @NotNull private Long academicYearId;
    @NotNull private Long timeSlotId;
    @NotNull private DayOfWeek dayOfWeek;
    private Semester semester;
}
