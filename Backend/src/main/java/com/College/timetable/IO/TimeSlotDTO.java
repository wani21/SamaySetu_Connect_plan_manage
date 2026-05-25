package com.College.timetable.IO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for time slot information in timetable export
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlotDTO {
    
    private Long id;
    
    private String slotName;
    
    private String startTime; // Format: "09:00"
    
    private String endTime; // Format: "10:00"
    
    private Boolean isBreak;
}
