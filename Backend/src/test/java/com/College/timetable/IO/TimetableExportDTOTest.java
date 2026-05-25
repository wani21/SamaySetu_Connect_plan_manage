package com.College.timetable.IO;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for timetable export DTOs
 */
class TimetableExportDTOTest {

    @Test
    void testTimeSlotDTO() {
        // Arrange & Act
        TimeSlotDTO timeSlot = new TimeSlotDTO(
            1L,
            "Slot 1",
            "09:00",
            "10:00",
            false
        );

        // Assert
        assertEquals(1L, timeSlot.getId());
        assertEquals("Slot 1", timeSlot.getSlotName());
        assertEquals("09:00", timeSlot.getStartTime());
        assertEquals("10:00", timeSlot.getEndTime());
        assertFalse(timeSlot.getIsBreak());
    }

    @Test
    void testTimeSlotDTOBreak() {
        // Arrange & Act
        TimeSlotDTO breakSlot = new TimeSlotDTO(
            2L,
            "Break",
            "11:00",
            "11:15",
            true
        );

        // Assert
        assertTrue(breakSlot.getIsBreak());
    }

    @Test
    void testTimetableEntryDTO() {
        // Arrange & Act
        TimetableEntryDTO entry = new TimetableEntryDTO(
            1L,
            "Data Structures",
            "CS 3 A",
            3, // divisionYear
            null,
            "Dr. Smith",
            "Room 101",
            "R101",
            "MONDAY",
            1L,
            false
        );

        // Assert
        assertEquals(1L, entry.getId());
        assertEquals("Data Structures", entry.getCourseName());
        assertEquals("CS 3 A", entry.getDivisionName());
        assertEquals(3, entry.getDivisionYear());
        assertNull(entry.getBatchName());
        assertEquals("Dr. Smith", entry.getProfessorName());
        assertEquals("Room 101", entry.getRoomName());
        assertEquals("R101", entry.getRoomNumber());
        assertEquals("MONDAY", entry.getDayOfWeek());
        assertEquals(1L, entry.getTimeSlotId());
        assertFalse(entry.getIsLabSession());
    }

    @Test
    void testTimetableEntryDTOLabSession() {
        // Arrange & Act
        TimetableEntryDTO labEntry = new TimetableEntryDTO(
            2L,
            "Database Lab",
            "CS 3 A",
            3, // divisionYear
            "Batch 1",
            "Dr. Johnson",
            "Lab 201",
            "L201",
            "TUESDAY",
            3L,
            true
        );

        // Assert
        assertEquals("Batch 1", labEntry.getBatchName());
        assertTrue(labEntry.getIsLabSession());
    }

    @Test
    void testTimetableExportDTO() {
        // Arrange
        List<TimeSlotDTO> timeSlots = new ArrayList<>();
        timeSlots.add(new TimeSlotDTO(1L, "Slot 1", "09:00", "10:00", false));
        timeSlots.add(new TimeSlotDTO(2L, "Slot 2", "10:00", "11:00", false));

        List<TimetableEntryDTO> entries = new ArrayList<>();
        entries.add(new TimetableEntryDTO(
            1L, "Data Structures", "CS 3 A", 3, null, 
            "Dr. Smith", "Room 101", "R101", "MONDAY", 1L, false
        ));

        // Act
        TimetableExportDTO exportDTO = new TimetableExportDTO(
            "Dr. John Doe",
            "EMP001",
            "2023-2024",
            "Semester 1",
            timeSlots,
            entries
        );

        // Assert
        assertEquals("Dr. John Doe", exportDTO.getEntityName());
        assertEquals("EMP001", exportDTO.getEntityIdentifier());
        assertEquals("2023-2024", exportDTO.getAcademicYearName());
        assertEquals("Semester 1", exportDTO.getSemesterLabel());
        assertEquals(2, exportDTO.getTimeSlots().size());
        assertEquals(1, exportDTO.getEntries().size());
    }

    @Test
    void testTimetableExportDTOForRoom() {
        // Arrange
        List<TimeSlotDTO> timeSlots = new ArrayList<>();
        List<TimetableEntryDTO> entries = new ArrayList<>();
        entries.add(new TimetableEntryDTO(
            1L, "Algorithms", "CS 2 B", 2, null, 
            "Dr. Williams", "Lab 301", "L301", "WEDNESDAY", 2L, false
        ));

        // Act
        TimetableExportDTO roomExportDTO = new TimetableExportDTO(
            "Computer Lab 1",
            "L301",
            "2023-2024",
            "Semester 2",
            timeSlots,
            entries
        );

        // Assert
        assertEquals("Computer Lab 1", roomExportDTO.getEntityName());
        assertEquals("L301", roomExportDTO.getEntityIdentifier());
        assertEquals("Semester 2", roomExportDTO.getSemesterLabel());
    }

    @Test
    void testDTOSettersAndGetters() {
        // Test TimeSlotDTO
        TimeSlotDTO timeSlot = new TimeSlotDTO();
        timeSlot.setId(5L);
        timeSlot.setSlotName("Slot 5");
        timeSlot.setStartTime("14:00");
        timeSlot.setEndTime("15:00");
        timeSlot.setIsBreak(false);

        assertEquals(5L, timeSlot.getId());
        assertEquals("Slot 5", timeSlot.getSlotName());

        // Test TimetableEntryDTO
        TimetableEntryDTO entry = new TimetableEntryDTO();
        entry.setId(10L);
        entry.setCourseName("Operating Systems");
        entry.setDivisionName("CS 4 A");
        entry.setDivisionYear(4);
        entry.setBatchName("Batch 2");
        entry.setProfessorName("Dr. Brown");
        entry.setRoomName("Room 202");
        entry.setRoomNumber("R202");
        entry.setDayOfWeek("FRIDAY");
        entry.setTimeSlotId(5L);
        entry.setIsLabSession(true);

        assertEquals(10L, entry.getId());
        assertEquals("Operating Systems", entry.getCourseName());
        assertEquals(4, entry.getDivisionYear());
        assertEquals("Batch 2", entry.getBatchName());

        // Test TimetableExportDTO
        TimetableExportDTO exportDTO = new TimetableExportDTO();
        exportDTO.setEntityName("Dr. Test");
        exportDTO.setEntityIdentifier("TEST001");
        exportDTO.setAcademicYearName("2024-2025");
        exportDTO.setSemesterLabel("Semester 1");
        exportDTO.setTimeSlots(new ArrayList<>());
        exportDTO.setEntries(new ArrayList<>());

        assertEquals("Dr. Test", exportDTO.getEntityName());
        assertEquals("TEST001", exportDTO.getEntityIdentifier());
        assertNotNull(exportDTO.getTimeSlots());
        assertNotNull(exportDTO.getEntries());
    }

    @Test
    void testEmptyCollections() {
        // Arrange & Act
        TimetableExportDTO exportDTO = new TimetableExportDTO(
            "Test Entity",
            "TEST001",
            "2023-2024",
            "Semester 1",
            new ArrayList<>(),
            new ArrayList<>()
        );

        // Assert
        assertNotNull(exportDTO.getTimeSlots());
        assertNotNull(exportDTO.getEntries());
        assertTrue(exportDTO.getTimeSlots().isEmpty());
        assertTrue(exportDTO.getEntries().isEmpty());
    }
}
