package com.College.timetable.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.College.timetable.Entity.*;
import com.College.timetable.Repository.TimetableEntry_repo;
import com.College.timetable.Repository.TeacherAvailability_repo;
import com.College.timetable.Repository.Room_repo;
import com.College.timetable.Repository.Division_repo;
import com.College.timetable.Repository.TimeSlot_repo;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConflictCheckService {

    private final TimetableEntry_repo timetableEntryRepository;
    private final TeacherAvailability_repo availabilityRepo;
    private final Room_repo roomRepo;
    private final Division_repo divisionRepo;
    private final TimeSlot_repo timeSlotRepo;

    // Configurable limits — override in application.properties
    @Value("${app.timetable.max-periods-per-day:6}")
    private int maxPeriodsPerDay;

    /**
     * Check all conflicts for a new or updated timetable entry.
     * Returns ALL conflict messages at once so admin can fix everything in one pass.
     */
    public List<String> checkConflicts(TimetableEntryRequest request, Long excludeId) {
        List<String> conflicts = new ArrayList<>();

        boolean isLabEntry = request.getLabSessionGroupId() != null;

        // ── 0. BREAK SLOT PROTECTION ──
        Optional<TimeSlot> slotOpt = timeSlotRepo.findById(request.getTimeSlotId());
        if (slotOpt.isPresent() && Boolean.TRUE.equals(slotOpt.get().getIsBreak())) {
            conflicts.add(String.format(
                "Break slot: Cannot schedule a class during '%s' — this is a break period.",
                slotOpt.get().getSlotName()
            ));
            return conflicts; // No point checking further if it's a break slot
        }

        // ── 1. TEACHER CONFLICT — teacher already assigned at this time ──
        if (!isLabEntry) {
            boolean teacherBooked = timetableEntryRepository.isTeacherBooked(
                request.getTeacherId(),
                request.getDayOfWeek(),
                request.getTimeSlotId(),
                request.getAcademicYearId(),
                excludeId
            );
            if (teacherBooked) {
                conflicts.add(String.format(
                    "Teacher conflict: This teacher is already assigned to another class on %s at this time slot.",
                    request.getDayOfWeek()
                ));
            }
        }

        // ── 2. ROOM CONFLICT — always checked, even for labs ──
        boolean roomBooked = timetableEntryRepository.isRoomBooked(
            request.getRoomId(),
            request.getDayOfWeek(),
            request.getTimeSlotId(),
            request.getAcademicYearId(),
            excludeId
        );
        if (roomBooked) {
            conflicts.add(String.format(
                "Room conflict: This room is already booked on %s at this time slot.",
                request.getDayOfWeek()
            ));
        }

        // ── 3. DIVISION CONFLICT — division already has a class at this time ──
        if (!isLabEntry) {
            boolean divisionBooked = timetableEntryRepository.isDivisionBooked(
                request.getDivisionId(),
                request.getDayOfWeek(),
                request.getTimeSlotId(),
                request.getAcademicYearId(),
                excludeId
            );
            if (divisionBooked) {
                conflicts.add(String.format(
                    "Division conflict: This division already has a class on %s at this time slot.",
                    request.getDayOfWeek()
                ));
            }
        }

        // ── 4. TEACHER DAILY PERIOD LIMIT (configurable) ──
        if (!isLabEntry) {
            long dailyPeriods = timetableEntryRepository.countTeacherHoursOnDay(
                request.getTeacherId(),
                request.getAcademicYearId(),
                request.getDayOfWeek()
            );
            if (dailyPeriods >= maxPeriodsPerDay) {
                conflicts.add(String.format(
                    "Teacher daily limit: This teacher already has %d periods on %s. Maximum is %d per day.",
                    dailyPeriods, request.getDayOfWeek(), maxPeriodsPerDay
                ));
            }
        }

        // ── 5. TEACHER WEEKLY HOUR LIMIT (uses actual duration, not entry count) ──
        if (!isLabEntry && request.getTeacherMaxWeeklyHours() != null) {
            // FIX: Use SUM(durationMinutes) instead of COUNT(entries)
            Integer weeklyMinutes = timetableEntryRepository.calculateTeacherWeeklyMinutes(
                request.getTeacherId(),
                request.getAcademicYearId(),
                excludeId
            );
            int currentHours = (weeklyMinutes != null ? weeklyMinutes : 0) / 60;
            // Also add the duration of the slot being added
            int newSlotMinutes = 0;
            if (slotOpt.isPresent()) {
                newSlotMinutes = slotOpt.get().getDurationMinutes() != null ? slotOpt.get().getDurationMinutes() : 60;
            }
            int projectedMinutes = (weeklyMinutes != null ? weeklyMinutes : 0) + newSlotMinutes;
            int projectedHours = projectedMinutes / 60;

            if (projectedHours > request.getTeacherMaxWeeklyHours()) {
                conflicts.add(String.format(
                    "Teacher weekly limit: Adding this class would give this teacher %d hours/week (current: %d hrs). Maximum is %d hours.",
                    projectedHours, currentHours, request.getTeacherMaxWeeklyHours()
                ));
            }
        }

        // ── 6. ROOM CAPACITY vs DIVISION STRENGTH ──
        Optional<ClassRoom> roomOpt = roomRepo.findById(request.getRoomId());
        if (roomOpt.isPresent()) {
            ClassRoom room = roomOpt.get();

            if (isLabEntry && request.getBatchStrength() != null) {
                // For lab entries: check batch size fits in room
                if (room.getCapacity() != null && request.getBatchStrength() > room.getCapacity()) {
                    conflicts.add(String.format(
                        "Room capacity: Batch has %d students but room '%s' only holds %d.",
                        request.getBatchStrength(), room.getName(), room.getCapacity()
                    ));
                }
            } else {
                // For theory: check division strength fits in room
                Optional<Division> divOpt = divisionRepo.findById(request.getDivisionId());
                if (divOpt.isPresent() && divOpt.get().getTotalStudents() != null
                        && room.getCapacity() != null
                        && divOpt.get().getTotalStudents() > room.getCapacity()) {
                    conflicts.add(String.format(
                        "Room capacity: Division has %d students but room '%s' only holds %d. Consider a larger room.",
                        divOpt.get().getTotalStudents(), room.getName(), room.getCapacity()
                    ));
                }
            }

            // ── 7. ROOM TYPE ↔ COURSE TYPE MATCHING (warning, not blocking) ──
            if (request.getCourseType() != null && room.getRoomType() != null) {
                String courseType = request.getCourseType().toUpperCase();
                String roomType = room.getRoomType().name().toUpperCase();

                boolean mismatch = false;
                if ("LAB".equals(courseType) && !"LAB".equals(roomType)) {
                    mismatch = true;
                } else if ("THEORY".equals(courseType) && "LAB".equals(roomType)) {
                    mismatch = true;
                }

                if (mismatch) {
                    conflicts.add(String.format(
                        "Room type warning: Course is '%s' but room '%s' is type '%s'. Consider using a %s instead.",
                        courseType, room.getName(), roomType,
                        "LAB".equals(courseType) ? "lab room" : "classroom"
                    ));
                }
            }
        }

        // ── 8. TEACHER AVAILABILITY CHECK ──
        if (!isLabEntry && slotOpt.isPresent()) {
            TimeSlot slot = slotOpt.get();
            try {
                long availCount = availabilityRepo.countAvailability(
                    request.getTeacherId(),
                    request.getDayOfWeek(),
                    slot.getStartTime(),
                    slot.getEndTime()
                );
                // If teacher has explicitly set availability AND marked unavailable
                List<TeacherAvailability> dayAvailability = availabilityRepo
                    .findByTeacherIdAndDayOfWeek(request.getTeacherId(), request.getDayOfWeek());
                if (!dayAvailability.isEmpty() && availCount == 0) {
                    conflicts.add(String.format(
                        "Teacher availability: This teacher is marked as unavailable on %s at this time slot.",
                        request.getDayOfWeek()
                    ));
                }
            } catch (Exception e) {
                // Don't block scheduling if availability check fails
            }
        }

        return conflicts;
    }

    /**
     * Quick check — returns true if ANY conflict exists
     */
    public boolean hasConflicts(TimetableEntryRequest request, Long excludeId) {
        return !checkConflicts(request, excludeId).isEmpty();
    }

    // ---------------------------------------------------------------
    // Inner DTO — carries all data needed for conflict checks
    // ---------------------------------------------------------------
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TimetableEntryRequest {
        private Long teacherId;
        private Long roomId;
        private Long divisionId;
        private Long timeSlotId;
        private Long academicYearId;
        private Long labSessionGroupId;   // null for theory, non-null for lab entries
        private DayOfWeek dayOfWeek;
        private Integer teacherMaxWeeklyHours;
        private Integer batchStrength;    // for lab entries — number of students in batch
        private String courseType;        // THEORY or LAB — for room type matching
    }
}
