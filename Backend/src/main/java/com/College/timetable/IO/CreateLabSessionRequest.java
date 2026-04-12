package com.College.timetable.IO;

import com.College.timetable.Entity.DayOfWeek;
import com.College.timetable.Entity.Semester;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Single-step lab session creation — creates group + all batch entries at once.
 *
 * Example payload:
 * {
 *   "divisionId": 1, "courseId": 2, "academicYearId": 2,
 *   "timeSlotId": 1, "dayOfWeek": "MONDAY", "semester": "SEM_3",
 *   "batchAssignments": [
 *     { "batchId": 1, "teacherId": 3, "roomId": 8 },
 *     { "batchId": 2, "teacherId": 4, "roomId": 9 },
 *     { "batchId": 3, "teacherId": 5, "roomId": 10 }
 *   ]
 * }
 */
@Data
public class CreateLabSessionRequest {

    @NotNull(message = "Division is required")
    private Long divisionId;

    @NotNull(message = "Course is required")
    private Long courseId;

    @NotNull(message = "Academic year is required")
    private Long academicYearId;

    @NotNull(message = "Time slot is required")
    private Long timeSlotId;

    @NotNull(message = "Day of week is required")
    private DayOfWeek dayOfWeek;

    private Semester semester;

    @NotNull(message = "At least one batch assignment is required")
    @Size(min = 1, message = "At least one batch assignment is required")
    private List<BatchAssignment> batchAssignments;

    @Data
    public static class BatchAssignment {
        @NotNull(message = "Batch is required")
        private Long batchId;

        @NotNull(message = "Teacher is required")
        private Long teacherId;

        @NotNull(message = "Room is required")
        private Long roomId;
    }
}
