package com.College.timetable.IO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for timetable entry information in timetable export
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimetableEntryDTO {
    
    private Long id;
    
    private String courseName;
    
    private String divisionName; // Format: "CS 3 A" (branch year division)
    
    private Integer divisionYear; // Year of the division (1, 2, 3, 4)
    
    private String batchName; // For lab sessions only
    
    private String professorName; // For room view
    
    private String roomName; // For professor view
    
    private String roomNumber;
    
    private String dayOfWeek;
    
    private Long timeSlotId;
    
    private Boolean isLabSession;
}
