package com.College.timetable.Service;

import com.College.timetable.Entity.*;
import com.College.timetable.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Pre-publish validation for timetables.
 * Runs a comprehensive check before a timetable is published to ensure completeness
 * and correctness. Returns structured errors (blocking) and warnings (informational).
 */
@Service
@RequiredArgsConstructor
public class TimetableValidationService {

    private final TimetableEntry_repo timetableRepo;
    private final TimeSlot_repo timeSlotRepo;
    private final Division_repo divisionRepo;
    private final Teacher_Repo teacherRepo;

    /**
     * Validate a division's draft timetable before publishing.
     * Returns a ValidationResult with errors (block publish) and warnings (allow but flag).
     */
    @Transactional(readOnly = true)
    public ValidationResult validate(Long divisionId, Long academicYearId) {
        ValidationResult result = new ValidationResult();

        // Load draft entries for this division
        List<TimetableEntry> entries = timetableRepo.findByDivisionIdAndAcademicYearIdAndStatus(
            divisionId, academicYearId, TimetableStatus.DRAFT
        );

        if (entries.isEmpty()) {
            result.addError("NO_ENTRIES", "No draft entries found. Create timetable entries before publishing.");
            return result;
        }

        // Load reference data
        Division division = divisionRepo.findById(divisionId).orElse(null);
        List<TimeSlot> allSlots = timeSlotRepo.findAll().stream()
            .filter(s -> !Boolean.TRUE.equals(s.getIsBreak()))
            .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
            .collect(Collectors.toList());

        String slotType = division != null ? division.getTimeSlotType() : "TYPE_1";
        List<TimeSlot> divisionSlots = allSlots.stream()
            .filter(s -> slotType.equals(s.getType()))
            .sorted(Comparator.comparing(TimeSlot::getStartTime))
            .collect(Collectors.toList());

        // ── Check 1: Empty days ──
        Set<DayOfWeek> daysWithEntries = entries.stream()
            .map(TimetableEntry::getDayOfWeek)
            .collect(Collectors.toSet());

        for (DayOfWeek day : DayOfWeek.values()) {
            if (!daysWithEntries.contains(day)) {
                result.addWarning("EMPTY_DAY",
                    day + " has no classes scheduled. Is this intentional?");
            }
        }

        // ── Check 2: Empty slots during core hours (Period 1-4) ──
        List<TimeSlot> coreSlots = divisionSlots.stream()
            .limit(4) // First 4 lecture periods are "core"
            .collect(Collectors.toList());

        for (DayOfWeek day : daysWithEntries) {
            for (TimeSlot slot : coreSlots) {
                boolean hasEntry = entries.stream().anyMatch(e ->
                    e.getDayOfWeek() == day && e.getTimeSlot() != null &&
                    e.getTimeSlot().getId().equals(slot.getId()));
                if (!hasEntry) {
                    result.addWarning("EMPTY_CORE_SLOT",
                        day + " " + slot.getSlotName() + " (" + slot.getStartTime() + "-" + slot.getEndTime() +
                        ") is empty during core hours.");
                }
            }
        }

        // ── Check 3: Teacher weekly hours exceeded ──
        Map<Long, List<TimetableEntry>> entriesByTeacher = entries.stream()
            .filter(e -> e.getTeacher() != null)
            .collect(Collectors.groupingBy(e -> e.getTeacher().getId()));

        for (Map.Entry<Long, List<TimetableEntry>> teacherEntries : entriesByTeacher.entrySet()) {
            TeacherEntity teacher = teacherEntries.getValue().get(0).getTeacher();
            int totalMinutes = teacherEntries.getValue().stream()
                .mapToInt(e -> e.getTimeSlot() != null && e.getTimeSlot().getDurationMinutes() != null
                    ? e.getTimeSlot().getDurationMinutes() : 60)
                .sum();
            int totalHours = totalMinutes / 60;

            if (teacher.getMaxWeeklyHours() != null && totalHours > teacher.getMaxWeeklyHours()) {
                result.addError("TEACHER_HOURS_EXCEEDED",
                    teacher.getName() + " has " + totalHours + " hours/week but maximum is " +
                    teacher.getMaxWeeklyHours() + ". Remove some classes before publishing.");
            } else if (teacher.getMinWeeklyHours() != null && totalHours < teacher.getMinWeeklyHours()) {
                result.addWarning("TEACHER_HOURS_LOW",
                    teacher.getName() + " has only " + totalHours + " hours/week (minimum recommended: " +
                    teacher.getMinWeeklyHours() + ").");
            }
        }

        // ── Check 4: Teacher daily overload (>4 consecutive periods) ──
        for (Map.Entry<Long, List<TimetableEntry>> teacherEntries : entriesByTeacher.entrySet()) {
            TeacherEntity teacher = teacherEntries.getValue().get(0).getTeacher();
            Map<DayOfWeek, Long> periodsPerDay = teacherEntries.getValue().stream()
                .collect(Collectors.groupingBy(TimetableEntry::getDayOfWeek, Collectors.counting()));

            for (Map.Entry<DayOfWeek, Long> dayCount : periodsPerDay.entrySet()) {
                if (dayCount.getValue() > 5) {
                    result.addWarning("TEACHER_DAILY_HEAVY",
                        teacher.getName() + " has " + dayCount.getValue() + " periods on " +
                        dayCount.getKey() + ". Consider redistributing for better workload balance.");
                }
            }
        }

        // ── Check 5: Room double-booking within draft ──
        // Group entries by day+slot+room and check for duplicates (excluding lab group entries)
        Map<String, List<TimetableEntry>> roomSlotMap = entries.stream()
            .filter(e -> e.getRoom() != null && e.getTimeSlot() != null)
            .collect(Collectors.groupingBy(e ->
                e.getDayOfWeek() + ":" + e.getTimeSlot().getId() + ":" + e.getRoom().getId()));

        for (Map.Entry<String, List<TimetableEntry>> slot : roomSlotMap.entrySet()) {
            if (slot.getValue().size() > 1) {
                // Check if they're all from the same lab group (intentional)
                boolean allSameLabGroup = slot.getValue().stream()
                    .allMatch(e -> e.getLabSessionGroup() != null &&
                        e.getLabSessionGroup().getId().equals(slot.getValue().get(0).getLabSessionGroup().getId()));
                if (!allSameLabGroup) {
                    TimetableEntry first = slot.getValue().get(0);
                    result.addError("ROOM_DOUBLE_BOOKED",
                        "Room " + first.getRoom().getName() + " is double-booked on " +
                        first.getDayOfWeek() + " at " + first.getTimeSlot().getSlotName() + ".");
                }
            }
        }

        // ── Check 6: Summary stats ──
        result.setTotalEntries(entries.size());
        result.setTotalTeachers(entriesByTeacher.size());
        result.setDaysScheduled(daysWithEntries.size());
        result.setDivisionName(division != null ? (division.getName() + " - Year " + division.getYear()) : "Unknown");

        return result;
    }

    // ── Result DTO ──

    @lombok.Data
    public static class ValidationResult {
        private List<ValidationItem> errors = new ArrayList<>();
        private List<ValidationItem> warnings = new ArrayList<>();
        private int totalEntries;
        private int totalTeachers;
        private int daysScheduled;
        private String divisionName;

        public void addError(String code, String message) {
            errors.add(new ValidationItem(code, message));
        }

        public void addWarning(String code, String message) {
            warnings.add(new ValidationItem(code, message));
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public boolean isPublishable() {
            return errors.isEmpty();
        }
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ValidationItem {
        private String code;
        private String message;
    }
}
