package com.College.timetable.IO;

import com.College.timetable.Entity.DayOfWeek;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;

@Data
public class ManualTimetableRequest {
	
	 private Long divisionId;
	    private Long courseId;
	    private Long teacherId;
	    private Long roomId;
	    private Long timeSlotId;
	    private Long academicYearId;

	    private DayOfWeek dayOfWeek;

	    private Integer weekNumber = 1;
	    private Boolean isRecurring = true;
	    private String notes;

}
