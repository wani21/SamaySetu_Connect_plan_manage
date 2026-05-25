package com.College.timetable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.College.timetable.Entity.*;
import com.College.timetable.Repository.*;
import com.College.timetable.Service.ConflictCheckService;
import com.College.timetable.Service.ConflictCheckService.TimetableEntryRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ConflictCheckServiceTest {

    @Mock
    private TimetableEntry_repo timetableEntryRepository;

    @Mock
    private TeacherAvailability_repo availabilityRepo;

    @Mock
    private Room_repo roomRepo;

    @Mock
    private Division_repo divisionRepo;

    @Mock
    private TimeSlot_repo timeSlotRepo;

    @Mock
    private Batch_repo batchRepo;

    @InjectMocks
    private ConflictCheckService conflictCheckService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(conflictCheckService, "maxPeriodsPerDay", 6);
    }

    @Test
    void testTheoryCourseExceedsRoomCapacity_ShouldFail() {
        // Arrange
        Long roomId = 1L;
        Long divisionId = 2L;
        Long timeSlotId = 3L;

        ClassRoom room = new ClassRoom();
        room.setId(roomId);
        room.setName("TheoryRoom");
        room.setCapacity(30);
        room.setIsActive(true);

        Division division = new Division();
        division.setId(divisionId);
        division.setTotalStudents(72);

        TimeSlot timeSlot = new TimeSlot();
        timeSlot.setId(timeSlotId);
        timeSlot.setIsBreak(false);

        when(timeSlotRepo.findById(timeSlotId)).thenReturn(Optional.of(timeSlot));
        when(roomRepo.findById(roomId)).thenReturn(Optional.of(room));
        when(divisionRepo.findById(divisionId)).thenReturn(Optional.of(division));

        TimetableEntryRequest request = TimetableEntryRequest.builder()
                .roomId(roomId)
                .divisionId(divisionId)
                .timeSlotId(timeSlotId)
                .courseType("THEORY")
                .dayOfWeek(DayOfWeek.MONDAY)
                .build();

        // Act
        List<String> conflicts = conflictCheckService.checkConflicts(request, null);

        // Assert
        assertFalse(conflicts.isEmpty());
        assertTrue(conflicts.stream().anyMatch(c -> c.contains("Room capacity: Division has 72 students but room 'TheoryRoom' only holds 30.")));
    }

    @Test
    void testLabCourseWithSpecificBatchCapacityFitsRoom_ShouldSucceed() {
        // Arrange
        Long roomId = 1L;
        Long divisionId = 2L;
        Long timeSlotId = 3L;
        Long batchId = 4L;

        ClassRoom room = new ClassRoom();
        room.setId(roomId);
        room.setName("LabRoom");
        room.setCapacity(30);
        room.setIsActive(true);

        Division division = new Division();
        division.setId(divisionId);
        division.setTotalStudents(72);

        TimeSlot timeSlot = new TimeSlot();
        timeSlot.setId(timeSlotId);
        timeSlot.setIsBreak(false);

        Batch batch = new Batch();
        batch.setId(batchId);
        batch.setStrength(24);

        when(timeSlotRepo.findById(timeSlotId)).thenReturn(Optional.of(timeSlot));
        when(roomRepo.findById(roomId)).thenReturn(Optional.of(room));
        when(divisionRepo.findById(divisionId)).thenReturn(Optional.of(division));
        when(batchRepo.findById(batchId)).thenReturn(Optional.of(batch));

        TimetableEntryRequest request = TimetableEntryRequest.builder()
                .roomId(roomId)
                .divisionId(divisionId)
                .timeSlotId(timeSlotId)
                .batchId(batchId)
                .courseType("LAB")
                .dayOfWeek(DayOfWeek.MONDAY)
                .build();

        // Act
        List<String> conflicts = conflictCheckService.checkConflicts(request, null);

        // Assert
        assertTrue(conflicts.stream().noneMatch(c -> c.contains("Room capacity:")));
    }

    @Test
    void testLabCourseWithFallbackCapacityFitsRoom_ShouldSucceed() {
        // Arrange
        Long roomId = 1L;
        Long divisionId = 2L;
        Long timeSlotId = 3L;
        Long batchId = 4L;

        ClassRoom room = new ClassRoom();
        room.setId(roomId);
        room.setName("LabRoom");
        room.setCapacity(30);
        room.setIsActive(true);

        Division division = new Division();
        division.setId(divisionId);
        division.setTotalStudents(72);

        TimeSlot timeSlot = new TimeSlot();
        timeSlot.setId(timeSlotId);
        timeSlot.setIsBreak(false);

        Batch batch = new Batch();
        batch.setId(batchId);
        batch.setStrength(null); // Force fallback

        when(timeSlotRepo.findById(timeSlotId)).thenReturn(Optional.of(timeSlot));
        when(roomRepo.findById(roomId)).thenReturn(Optional.of(room));
        when(divisionRepo.findById(divisionId)).thenReturn(Optional.of(division));
        when(batchRepo.findById(batchId)).thenReturn(Optional.of(batch));
        when(batchRepo.countByDivisionId(divisionId)).thenReturn(3L); // 72 / 3 = 24

        TimetableEntryRequest request = TimetableEntryRequest.builder()
                .roomId(roomId)
                .divisionId(divisionId)
                .timeSlotId(timeSlotId)
                .batchId(batchId)
                .courseType("LAB")
                .dayOfWeek(DayOfWeek.MONDAY)
                .build();

        // Act
        List<String> conflicts = conflictCheckService.checkConflicts(request, null);

        // Assert
        assertTrue(conflicts.stream().noneMatch(c -> c.contains("Room capacity:")));
    }

    @Test
    void testLabCourseWithFallbackCapacityExceedsRoom_ShouldFail() {
        // Arrange
        Long roomId = 1L;
        Long divisionId = 2L;
        Long timeSlotId = 3L;
        Long batchId = 4L;

        ClassRoom room = new ClassRoom();
        room.setId(roomId);
        room.setName("SmallLabRoom");
        room.setCapacity(20);
        room.setIsActive(true);

        Division division = new Division();
        division.setId(divisionId);
        division.setTotalStudents(72);

        TimeSlot timeSlot = new TimeSlot();
        timeSlot.setId(timeSlotId);
        timeSlot.setIsBreak(false);

        Batch batch = new Batch();
        batch.setId(batchId);
        batch.setStrength(null); // Force fallback

        when(timeSlotRepo.findById(timeSlotId)).thenReturn(Optional.of(timeSlot));
        when(roomRepo.findById(roomId)).thenReturn(Optional.of(room));
        when(divisionRepo.findById(divisionId)).thenReturn(Optional.of(division));
        when(batchRepo.findById(batchId)).thenReturn(Optional.of(batch));
        when(batchRepo.countByDivisionId(divisionId)).thenReturn(3L); // 72 / 3 = 24 required

        TimetableEntryRequest request = TimetableEntryRequest.builder()
                .roomId(roomId)
                .divisionId(divisionId)
                .timeSlotId(timeSlotId)
                .batchId(batchId)
                .courseType("LAB")
                .dayOfWeek(DayOfWeek.MONDAY)
                .build();

        // Act
        List<String> conflicts = conflictCheckService.checkConflicts(request, null);

        // Assert
        assertFalse(conflicts.isEmpty());
        assertTrue(conflicts.stream().anyMatch(c -> c.contains("Room capacity: Batch has 24 students but room 'SmallLabRoom' only holds 20.")));
    }
}
