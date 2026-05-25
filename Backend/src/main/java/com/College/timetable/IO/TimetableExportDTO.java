package com.College.timetable.IO;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for timetable export data containing entity information, time slots, and entries
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimetableExportDTO {
    
    private String entityName;
    
    private String entityIdentifier; // employee_id for professor or room_number for room
    
    private String academicYearName;
    
    private String semesterLabel;
    
    private List<TimeSlotDTO> timeSlots;
    
    private List<TimetableEntryDTO> entries;
}
