package com.College.timetable.Service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import com.College.timetable.Entity.AcademicYear;
import com.College.timetable.Entity.Batch;
import com.College.timetable.Entity.ClassRoom;
import com.College.timetable.Entity.CourseEntity;
import com.College.timetable.Entity.Division;
import com.College.timetable.Entity.LabSessionGroup;
import com.College.timetable.Entity.TeacherEntity;
import com.College.timetable.Entity.TimeSlot;
import com.College.timetable.Entity.TimetableEntry;
import com.College.timetable.Entity.TimetableStatus;
import com.College.timetable.IO.CreateLabSessionGroupDTO;
import com.College.timetable.IO.CreateTimetableEntryDTO;
import com.College.timetable.Repository.AcademicYearRepository;
import com.College.timetable.Repository.Batch_repo;
import com.College.timetable.Repository.Course_repo;
import com.College.timetable.Repository.Division_repo;
import com.College.timetable.Repository.Lab_session_repo;
import com.College.timetable.Repository.Room_repo;
import com.College.timetable.Repository.Teacher_Repo;
import com.College.timetable.Repository.TimeSlot_repo;
import com.College.timetable.Repository.TimetableEntry_repo;
import com.College.timetable.Service.ConflictCheckService.TimetableEntryRequest;
import com.College.timetable.Util.TimetableConflictException;

@Service
@Transactional(readOnly = true)
public class TimetableService {

    @Autowired
    private TimetableEntry_repo timetableRepo;
    
    @Autowired
    private ConflictCheckService conflictCheckService;
    
    @Autowired
    private Course_repo courseRepository;
    
    @Autowired
    private Room_repo classRoomRepository;
    
    @Autowired
    private Lab_session_repo labSessionGroupRepository;
    
    @Autowired
    private TimeSlot_repo timeSlotRepository;
    
    @Autowired
    private Batch_repo batchRepository;
    
    @Autowired
    private Teacher_Repo teacherRepository;  
    
    @Autowired
    private AcademicYearRepository academicYearRepository;
    
    @Autowired
    private Division_repo divisionRepository;

    @Cacheable(value = "timetable-division", key = "{#divisionId, #academicYearId}")
    public List<TimetableEntry> getTimetableForDivision(Long divisionId, Long academicYearId) {
        return timetableRepo.findByDivisionIdAndAcademicYearId(divisionId, academicYearId);
    }

    @Cacheable(value = "timetable-teacher", key = "{#teacherId, #academicYearId}")
    public List<TimetableEntry> getTimetableForTeacher(Long teacherId, Long academicYearId) {
        return timetableRepo.findByTeacherIdAndAcademicYearId(teacherId, academicYearId);
    }
    
    @Transactional
    @CacheEvict(value = {"divisionTimetable", "teacherTimetable"}, allEntries = true)
    public TimetableEntry addEntry(CreateTimetableEntryDTO dto) {

        // 1. Build conflict check request with all context needed for validation
        var teacherOpt = teacherRepository.findById(dto.getTeacherId());
        var courseOpt = courseRepository.findById(dto.getCourseId());

        TimetableEntryRequest conflictRequest = TimetableEntryRequest.builder()
            .teacherId(dto.getTeacherId())
            .roomId(dto.getRoomId())
            .divisionId(dto.getDivisionId())
            .timeSlotId(dto.getTimeSlotId())
            .academicYearId(dto.getAcademicYearId())
            .labSessionGroupId(dto.getLabSessionGroupId())
            .dayOfWeek(dto.getDayOfWeek())
            .teacherMaxWeeklyHours(
                teacherOpt.map(TeacherEntity::getMaxWeeklyHours).orElse(null)
            )
            .courseType(
                courseOpt.map(c -> c.getCourseType() != null ? c.getCourseType().name() : null).orElse(null)
            )
            .batchStrength(null) // TODO: populate from batch entity when lab session
            .build();

        // 2. Check ALL conflicts — collect all messages before failing
        List<String> conflicts = conflictCheckService.checkConflicts(conflictRequest, null);
        if (!conflicts.isEmpty()) {
            throw new TimetableConflictException(conflicts);
        }

        // 3. Load all related entities
        Division division = divisionRepository.findById(dto.getDivisionId())
            .orElseThrow(() -> new RuntimeException("Division not found: " + dto.getDivisionId()));
        CourseEntity course = courseRepository.findById(dto.getCourseId())
            .orElseThrow(() -> new RuntimeException("Course not found: " + dto.getCourseId()));
        TeacherEntity teacher = teacherRepository.findById(dto.getTeacherId())
            .orElseThrow(() -> new RuntimeException("Teacher not found: " + dto.getTeacherId()));
        ClassRoom room = classRoomRepository.findById(dto.getRoomId())
            .orElseThrow(() -> new RuntimeException("Room not found: " + dto.getRoomId()));
        TimeSlot timeSlot = timeSlotRepository.findById(dto.getTimeSlotId())
            .orElseThrow(() -> new RuntimeException("Time slot not found: " + dto.getTimeSlotId()));
        AcademicYear academicYear = academicYearRepository.findById(dto.getAcademicYearId())
            .orElseThrow(() -> new RuntimeException("Academic year not found: " + dto.getAcademicYearId()));

        // 4. Build the entry
        TimetableEntry entry = new TimetableEntry();
        entry.setDivision(division);
        entry.setCourse(course);
        entry.setTeacher(teacher);
        entry.setRoom(room);
        entry.setTimeSlot(timeSlot);
        entry.setDayOfWeek(dto.getDayOfWeek());
        entry.setAcademicYear(academicYear);
        entry.setSemester(dto.getSemester());
        entry.setNotes(dto.getNotes());
        entry.setIsRecurring(true);
        entry.setWeekNumber(1);
        entry.setStatus(TimetableStatus.DRAFT);  // Always starts as DRAFT

        // 5. Handle lab session entries
        if (dto.getLabSessionGroupId() != null) {
            LabSessionGroup group = labSessionGroupRepository.findById(dto.getLabSessionGroupId())
                .orElseThrow(() -> new RuntimeException("Lab session group not found"));
            entry.setLabSessionGroup(group);
            entry.setIsLabSession(true);

            if (dto.getBatchId() != null) {
                Batch batch = batchRepository.findById(dto.getBatchId())
                    .orElseThrow(() -> new RuntimeException("Batch not found: " + dto.getBatchId()));
                entry.setBatch(batch);
            }
        }

        // 6. Save the primary entry
        TimetableEntry saved = timetableRepo.save(entry);

        // 7. AUTO-BOOK NEXT CONSECUTIVE SLOT FOR LAB COURSES
        // Lab sessions occupy 2 truly consecutive lecture periods with NO break in between.
        // If the immediate next slot is a break, the lab cannot be scheduled here.
        if (course.getCourseType() == com.College.timetable.Entity.CourseType.LAB) {
            String slotType = timeSlot.getType() != null ? timeSlot.getType() : "TYPE_1";

            // Find whatever slot comes immediately after (break or lecture)
            java.util.Optional<TimeSlot> immediateNextOpt = timeSlotRepository.findFirstByTypeAndIsActiveTrueAndStartTimeGreaterThanEqualOrderByStartTimeAsc(
                slotType, timeSlot.getEndTime()
            );

            if (immediateNextOpt.isEmpty()) {
                // No slot exists after this one — last period of the day
                timetableRepo.delete(saved);
                throw new TimetableConflictException(List.of(
                    "Lab scheduling error: No period exists after " + timeSlot.getSlotName() +
                    " (" + timeSlot.getEndTime() + "). Labs require 2 consecutive lecture periods. " +
                    "Choose an earlier slot."
                ));
            }

            TimeSlot nextSlot = immediateNextOpt.get();

            // If the immediate next slot is a break, reject — can't split a lab across a break
            if (Boolean.TRUE.equals(nextSlot.getIsBreak())) {
                timetableRepo.delete(saved);
                throw new TimetableConflictException(List.of(
                    "Lab scheduling error: The next slot after " + timeSlot.getSlotName() +
                    " is '" + nextSlot.getSlotName() + "' (break). " +
                    "Labs require 2 consecutive lecture periods with no break in between. " +
                    "Choose a different slot where two lecture periods are back-to-back."
                ));
            }

            // Next slot is a lecture period — check conflicts for it
            TimetableEntryRequest nextConflictReq = TimetableEntryRequest.builder()
                .teacherId(dto.getTeacherId())
                .roomId(dto.getRoomId())
                .divisionId(dto.getDivisionId())
                .timeSlotId(nextSlot.getId())
                .academicYearId(dto.getAcademicYearId())
                .labSessionGroupId(dto.getLabSessionGroupId())
                .dayOfWeek(dto.getDayOfWeek())
                .teacherMaxWeeklyHours(teacherOpt.map(TeacherEntity::getMaxWeeklyHours).orElse(null))
                .courseType("LAB")
                .build();

            List<String> nextConflicts = conflictCheckService.checkConflicts(nextConflictReq, null);
            if (!nextConflicts.isEmpty()) {
                timetableRepo.delete(saved);
                nextConflicts.add(0, "Lab requires 2 consecutive periods. The next slot (" + nextSlot.getSlotName() + ") has conflicts:");
                throw new TimetableConflictException(nextConflicts);
            }

            // All clear — create the second entry
            TimetableEntry secondEntry = new TimetableEntry();
            secondEntry.setDivision(division);
            secondEntry.setCourse(course);
            secondEntry.setTeacher(teacher);
            secondEntry.setRoom(room);
            secondEntry.setTimeSlot(nextSlot);
            secondEntry.setDayOfWeek(dto.getDayOfWeek());
            secondEntry.setAcademicYear(academicYear);
            secondEntry.setSemester(dto.getSemester());
            secondEntry.setNotes(dto.getNotes() != null ? dto.getNotes() : "Auto-booked: Lab period 2 of 2");
            secondEntry.setIsRecurring(true);
            secondEntry.setWeekNumber(1);
            secondEntry.setStatus(TimetableStatus.DRAFT);
            secondEntry.setIsLabSession(entry.getIsLabSession());
            secondEntry.setLabSessionGroup(entry.getLabSessionGroup());
            secondEntry.setBatch(entry.getBatch());

            timetableRepo.save(secondEntry);
        }

        return saved;
    }

    // ---------------------------------------------------------------
    // UPDATE — Admin edits an existing slot
    // ---------------------------------------------------------------

    @Transactional
    @CacheEvict(value = {"divisionTimetable", "teacherTimetable"}, allEntries = true)
    public TimetableEntry updateEntry(Long entryId, CreateTimetableEntryDTO dto) {

        TimetableEntry existing = timetableRepo.findById(entryId)
            .orElseThrow(() -> new RuntimeException("Timetable entry not found: " + entryId));

        // Only DRAFT entries can be edited
        if (existing.getStatus() == TimetableStatus.PUBLISHED) {
            throw new RuntimeException(
                "Cannot edit a published timetable entry. Archive it first or contact admin."
            );
        }

        // Conflict check — exclude this entry's own ID
        var updTeacherOpt = teacherRepository.findById(dto.getTeacherId());
        var updCourseOpt = courseRepository.findById(dto.getCourseId());

        TimetableEntryRequest conflictRequest = TimetableEntryRequest.builder()
            .teacherId(dto.getTeacherId())
            .roomId(dto.getRoomId())
            .divisionId(dto.getDivisionId())
            .timeSlotId(dto.getTimeSlotId())
            .academicYearId(dto.getAcademicYearId())
            .labSessionGroupId(dto.getLabSessionGroupId())
            .dayOfWeek(dto.getDayOfWeek())
            .teacherMaxWeeklyHours(
                updTeacherOpt.map(TeacherEntity::getMaxWeeklyHours).orElse(null)
            )
            .courseType(
                updCourseOpt.map(c -> c.getCourseType() != null ? c.getCourseType().name() : null).orElse(null)
            )
            .build();

        List<String> conflicts = conflictCheckService.checkConflicts(conflictRequest, entryId);
        if (!conflicts.isEmpty()) {
            throw new TimetableConflictException(conflicts);
        }

        // Apply updates
        existing.setTeacher(teacherRepository.findById(dto.getTeacherId()).orElseThrow());
        existing.setRoom(classRoomRepository.findById(dto.getRoomId()).orElseThrow());
        existing.setTimeSlot(timeSlotRepository.findById(dto.getTimeSlotId()).orElseThrow());
        existing.setDayOfWeek(dto.getDayOfWeek());
        existing.setNotes(dto.getNotes());
        existing.setSemester(dto.getSemester());

        return timetableRepo.save(existing);
    }

    // ---------------------------------------------------------------
    // DELETE — Remove a single DRAFT entry
    // ---------------------------------------------------------------

    @Transactional
    @CacheEvict(value = {"divisionTimetable", "teacherTimetable"}, allEntries = true)
    public void deleteEntry(Long entryId) {
        TimetableEntry entry = timetableRepo.findById(entryId)
            .orElseThrow(() -> new RuntimeException("Timetable entry not found: " + entryId));

        if (entry.getStatus() == TimetableStatus.PUBLISHED) {
            throw new RuntimeException("Cannot delete a published entry. Archive the timetable first.");
        }

        // If this is a LAB entry, also delete its paired consecutive-slot entry
        if (entry.getCourse() != null && entry.getCourse().getCourseType() == com.College.timetable.Entity.CourseType.LAB) {
            // Find entries with same course, teacher, room, day, division, academic year, status=DRAFT
            List<TimetableEntry> pairedEntries = timetableRepo.findByDivisionIdAndAcademicYearIdAndStatus(
                entry.getDivision().getId(),
                entry.getAcademicYear().getId(),
                TimetableStatus.DRAFT
            ).stream()
                .filter(e -> !e.getId().equals(entry.getId()))
                .filter(e -> e.getCourse() != null && e.getCourse().getId().equals(entry.getCourse().getId()))
                .filter(e -> e.getTeacher() != null && e.getTeacher().getId().equals(entry.getTeacher().getId()))
                .filter(e -> e.getRoom() != null && e.getRoom().getId().equals(entry.getRoom().getId()))
                .filter(e -> e.getDayOfWeek() == entry.getDayOfWeek())
                .toList();

            timetableRepo.deleteAll(pairedEntries);
        }

        timetableRepo.delete(entry);
    }

    // ---------------------------------------------------------------
    // PUBLISH — Admin reviewed DRAFT, now make it live
    // ---------------------------------------------------------------

    @Transactional
    @CacheEvict(value = {"divisionTimetable", "teacherTimetable"}, allEntries = true)
    public int publishTimetable(Long divisionId, Long academicYearId) {
        int count = timetableRepo.publishDivisionTimetable(divisionId, academicYearId);
        if (count == 0) {
            throw new RuntimeException("No draft entries found to publish for this division.");
        }
        return count;
    }

    // ---------------------------------------------------------------
    // ARCHIVE — End of semester, move PUBLISHED to ARCHIVED
    // ---------------------------------------------------------------

    @Transactional
    @CacheEvict(value = {"divisionTimetable", "teacherTimetable"}, allEntries = true)
    public int archiveTimetable(Long divisionId, Long academicYearId) {
        return timetableRepo.archiveDivisionTimetable(divisionId, academicYearId);
    }

    // ---------------------------------------------------------------
    // READ — Cached in Redis
    // ---------------------------------------------------------------

    /**
     * Get published timetable for a division — CACHED
     * Cache key: divisionTimetable::{divisionId}::{academicYearId}
     */
    @Cacheable(value = "divisionTimetable", key = "#divisionId + '_' + #academicYearId")
    public List<TimetableEntry> getDivisionTimetable(Long divisionId, Long academicYearId) {
        return timetableRepo
            .findByDivisionIdAndAcademicYearIdAndStatus(divisionId, academicYearId, TimetableStatus.PUBLISHED);
    }

    /**
     * Get published timetable for a teacher — CACHED
     */
    @Cacheable(value = "teacherTimetable", key = "#teacherId + '_' + #academicYearId")
    public List<TimetableEntry> getTeacherTimetable(Long teacherId, Long academicYearId) {
        return timetableRepo
            .findByTeacherIdAndAcademicYearIdAndStatus(teacherId, academicYearId, TimetableStatus.PUBLISHED);
    }

    /**
     * Get DRAFT timetable for admin review — NOT cached
     */
    public List<TimetableEntry> getDraftTimetable(Long divisionId, Long academicYearId) {
        return timetableRepo
            .findByDivisionIdAndAcademicYearIdAndStatusOrderByDayOfWeekAscTimeSlotAsc(
                divisionId, academicYearId, TimetableStatus.DRAFT);
    }

    /**
     * Clear all DRAFT entries for a division — start fresh
     */
    @Transactional
    @CacheEvict(value = {"divisionTimetable", "teacherTimetable"}, allEntries = true)
    public int clearDraft(Long divisionId, Long academicYearId) {
        return timetableRepo.clearDraftTimetable(divisionId, academicYearId);
    }

    // ---------------------------------------------------------------
    // LAB SESSION GROUP — Create the group before adding batch entries
    // ---------------------------------------------------------------

    @Transactional
    public LabSessionGroup createLabSessionGroup(CreateLabSessionGroupDTO dto) {
        LabSessionGroup group = new LabSessionGroup();
        group.setDivision(divisionRepository.findById(dto.getDivisionId()).orElseThrow());
        group.setCourse(courseRepository.findById(dto.getCourseId()).orElseThrow());
        group.setAcademicYear(academicYearRepository.findById(dto.getAcademicYearId()).orElseThrow());
        group.setTimeSlot(timeSlotRepository.findById(dto.getTimeSlotId()).orElseThrow());
        group.setDayOfWeek(dto.getDayOfWeek());
        group.setSemester(dto.getSemester());
        return labSessionGroupRepository.save(group);
    }

    /**
     * Copy all non-lab DRAFT entries from one division to another.
     * Preserves course, teacher, room, time slot, day — only changes division reference.
     * Skips lab sessions (they need batch-specific config for the target division).
     */
    @Transactional
    @CacheEvict(value = {"divisionTimetable", "teacherTimetable"}, allEntries = true)
    public int copyDraftEntries(Long sourceDivisionId, Long targetDivisionId, Long academicYearId) {
        if (sourceDivisionId.equals(targetDivisionId)) {
            throw new RuntimeException("Source and target division cannot be the same");
        }

        com.College.timetable.Entity.Division targetDivision = divisionRepository.findById(targetDivisionId)
                .orElseThrow(() -> new RuntimeException("Target division not found"));
        com.College.timetable.Entity.AcademicYear academicYear = academicYearRepository.findById(academicYearId)
                .orElseThrow(() -> new RuntimeException("Academic year not found"));

        // Get all DRAFT non-lab entries from source
        List<TimetableEntry> sourceEntries = timetableRepo
                .findByDivisionIdAndAcademicYearIdAndStatus(sourceDivisionId, academicYearId,
                    com.College.timetable.Entity.TimetableStatus.DRAFT)
                .stream()
                .filter(e -> !Boolean.TRUE.equals(e.getIsLabSession()))
                .toList();

        int copied = 0;
        for (TimetableEntry source : sourceEntries) {
            TimetableEntry copy = new TimetableEntry();
            copy.setDivision(targetDivision);
            copy.setAcademicYear(academicYear);
            copy.setCourse(source.getCourse());
            copy.setTeacher(source.getTeacher());
            copy.setRoom(source.getRoom());
            copy.setTimeSlot(source.getTimeSlot());
            copy.setDayOfWeek(source.getDayOfWeek());
            copy.setSemester(source.getSemester());
            copy.setStatus(com.College.timetable.Entity.TimetableStatus.DRAFT);
            copy.setIsLabSession(false);
            copy.setIsRecurring(true);
            copy.setWeekNumber(1);
            copy.setNotes(source.getNotes());

            timetableRepo.save(copy);
            copied++;
        }

        return copied;
    }

    // ---------------------------------------------------------------
    // LAB SESSION — Single-step creation (group + all batch entries)
    // ---------------------------------------------------------------

    @Transactional
    @CacheEvict(value = {"divisionTimetable", "teacherTimetable"}, allEntries = true)
    public Map<String, Object> createLabSession(com.College.timetable.IO.CreateLabSessionRequest request) {

        // 1. Load shared entities
        Division division = divisionRepository.findById(request.getDivisionId())
            .orElseThrow(() -> new RuntimeException("Division not found"));
        CourseEntity course = courseRepository.findById(request.getCourseId())
            .orElseThrow(() -> new RuntimeException("Course not found"));
        AcademicYear academicYear = academicYearRepository.findById(request.getAcademicYearId())
            .orElseThrow(() -> new RuntimeException("Academic year not found"));
        TimeSlot timeSlot = timeSlotRepository.findById(request.getTimeSlotId())
            .orElseThrow(() -> new RuntimeException("Time slot not found"));

        // 2. Validate: must be a LAB course
        if (course.getCourseType() != com.College.timetable.Entity.CourseType.LAB) {
            throw new RuntimeException("Lab sessions can only be created for LAB-type courses. '" + course.getName() + "' is " + course.getCourseType());
        }

        // 3. Validate: check next consecutive slot (lab = 2 periods, no break between)
        String slotType = timeSlot.getType() != null ? timeSlot.getType() : "TYPE_1";
        java.util.Optional<TimeSlot> immediateNextOpt = timeSlotRepository.findFirstByTypeAndIsActiveTrueAndStartTimeGreaterThanEqualOrderByStartTimeAsc(slotType, timeSlot.getEndTime());

        if (immediateNextOpt.isEmpty()) {
            throw new TimetableConflictException(List.of(
                "No period exists after " + timeSlot.getSlotName() + ". Labs require 2 consecutive lecture periods."
            ));
        }
        TimeSlot nextSlot = immediateNextOpt.get();
        if (Boolean.TRUE.equals(nextSlot.getIsBreak())) {
            throw new TimetableConflictException(List.of(
                "The next slot after " + timeSlot.getSlotName() + " is '" + nextSlot.getSlotName() + "' (break). " +
                "Labs require 2 consecutive lecture periods with no break in between."
            ));
        }

        // 4. Validate batch assignments: no duplicate rooms, no duplicate teachers
        var assignments = request.getBatchAssignments();
        java.util.Set<Long> roomIds = new java.util.HashSet<>();
        java.util.Set<Long> teacherIds = new java.util.HashSet<>();
        List<String> validationErrors = new java.util.ArrayList<>();

        for (var ba : assignments) {
            if (!roomIds.add(ba.getRoomId())) {
                validationErrors.add("Duplicate room: Two batches cannot use the same room for a lab session.");
            }
            if (!teacherIds.add(ba.getTeacherId())) {
                validationErrors.add("Duplicate teacher: Two batches cannot have the same teacher for a lab session.");
            }
        }
        if (!validationErrors.isEmpty()) {
            throw new TimetableConflictException(validationErrors);
        }

        // 5. Check conflicts for each batch on BOTH slots
        List<String> allConflicts = new java.util.ArrayList<>();
        for (int i = 0; i < assignments.size(); i++) {
            var ba = assignments.get(i);
            String batchLabel = "Batch " + (i + 1);

            // Check slot 1
            for (TimeSlot slot : List.of(timeSlot, nextSlot)) {
                boolean roomBooked = timetableRepo.isRoomBooked(ba.getRoomId(), request.getDayOfWeek(), slot.getId(), request.getAcademicYearId(), null);
                if (roomBooked) {
                    allConflicts.add(batchLabel + ": Room already booked on " + request.getDayOfWeek() + " at " + slot.getSlotName());
                }
                // Teacher conflict — only check if teacher is not already in another lab entry at this time
                boolean teacherBooked = timetableRepo.isTeacherBooked(ba.getTeacherId(), request.getDayOfWeek(), slot.getId(), request.getAcademicYearId(), null);
                if (teacherBooked) {
                    allConflicts.add(batchLabel + ": Teacher already assigned on " + request.getDayOfWeek() + " at " + slot.getSlotName());
                }
            }
        }
        // Division conflict — check if division already has a non-lab class at this slot
        boolean div1 = timetableRepo.isDivisionBooked(request.getDivisionId(), request.getDayOfWeek(), timeSlot.getId(), request.getAcademicYearId(), null);
        boolean div2 = timetableRepo.isDivisionBooked(request.getDivisionId(), request.getDayOfWeek(), nextSlot.getId(), request.getAcademicYearId(), null);
        if (div1) allConflicts.add("Division already has a class at " + timeSlot.getSlotName() + " on " + request.getDayOfWeek());
        if (div2) allConflicts.add("Division already has a class at " + nextSlot.getSlotName() + " on " + request.getDayOfWeek());

        if (!allConflicts.isEmpty()) {
            throw new TimetableConflictException(allConflicts);
        }

        // 6. Create the lab session group
        LabSessionGroup group = new LabSessionGroup();
        group.setDivision(division);
        group.setCourse(course);
        group.setAcademicYear(academicYear);
        group.setTimeSlot(timeSlot);
        group.setDayOfWeek(request.getDayOfWeek());
        group.setSemester(request.getSemester());
        group = labSessionGroupRepository.save(group);

        // 7. Create batch entries — 2 per batch (slot 1 + slot 2)
        int entriesCreated = 0;
        for (var ba : assignments) {
            TeacherEntity teacher = teacherRepository.findById(ba.getTeacherId())
                .orElseThrow(() -> new RuntimeException("Teacher not found: " + ba.getTeacherId()));
            ClassRoom room = classRoomRepository.findById(ba.getRoomId())
                .orElseThrow(() -> new RuntimeException("Room not found: " + ba.getRoomId()));
            Batch batch = batchRepository.findById(ba.getBatchId())
                .orElseThrow(() -> new RuntimeException("Batch not found: " + ba.getBatchId()));

            for (TimeSlot slot : List.of(timeSlot, nextSlot)) {
                TimetableEntry entry = new TimetableEntry();
                entry.setDivision(division);
                entry.setCourse(course);
                entry.setTeacher(teacher);
                entry.setRoom(room);
                entry.setTimeSlot(slot);
                entry.setDayOfWeek(request.getDayOfWeek());
                entry.setAcademicYear(academicYear);
                entry.setSemester(request.getSemester());
                entry.setStatus(TimetableStatus.DRAFT);
                entry.setIsLabSession(true);
                entry.setLabSessionGroup(group);
                entry.setBatch(batch);
                entry.setIsRecurring(true);
                entry.setWeekNumber(1);
                timetableRepo.save(entry);
                entriesCreated++;
            }
        }

        return Map.of(
            "message", "Lab session created successfully",
            "groupId", group.getId(),
            "entriesCreated", entriesCreated,
            "batches", assignments.size(),
            "slots", timeSlot.getSlotName() + " + " + nextSlot.getSlotName()
        );
    }

    /**
     * Delete an entire lab session group and all its entries.
     */
    @Transactional
    @CacheEvict(value = {"divisionTimetable", "teacherTimetable"}, allEntries = true)
    public int deleteLabSessionGroup(Long groupId) {
        List<TimetableEntry> entries = timetableRepo.findByLabSessionGroupId(groupId);
        int count = entries.size();
        timetableRepo.deleteAll(entries);
        labSessionGroupRepository.deleteById(groupId);
        return count;
    }
}
