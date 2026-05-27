package com.College.timetable.Controller;

import com.College.timetable.Entity.LabSessionGroup;
import com.College.timetable.Entity.Semester;
import com.College.timetable.Entity.TimetableEntry;
import com.College.timetable.IO.CreateLabSessionGroupDTO;
import com.College.timetable.IO.CreateLabSessionGroupDTO;
import com.College.timetable.IO.CreateTimetableEntryDTO;
import com.College.timetable.Service.TimetableService;
import com.College.timetable.Util.TimetableConflictException;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/timetable")
@RequiredArgsConstructor
public class TimetableController {

    private final TimetableService timetableService;
    private final com.College.timetable.Service.TimetableValidationService validationService;
    private final com.College.timetable.Service.TimetableExportService exportService;
    private final com.College.timetable.Service.DepartmentAuthorizationService authService;

    // ---------------------------------------------------------------
    // EXPORT endpoints — PDF & Excel download (semester-specific)
    // ---------------------------------------------------------------

    @GetMapping("/export/division/{divisionId}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'HOD', 'TIMETABLE_COORDINATOR')")
    public ResponseEntity<byte[]> exportDivisionPDF(
        @PathVariable Long divisionId,
        @RequestParam Long academicYearId,
        @RequestParam String semester
    ) {
        try {
            authService.checkDivisionAccess(divisionId);
            byte[] pdf = exportService.generateDivisionPDF(divisionId, academicYearId, Semester.valueOf(semester));
            return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=timetable_division_" + divisionId + "_" + semester + ".pdf")
                .body(pdf);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error generating division PDF: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/export/division/{divisionId}/excel")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'HOD', 'TIMETABLE_COORDINATOR')")
    public ResponseEntity<byte[]> exportDivisionExcel(
        @PathVariable Long divisionId,
        @RequestParam Long academicYearId,
        @RequestParam String semester
    ) {
        try {
            authService.checkDivisionAccess(divisionId);
            byte[] excel = exportService.generateDivisionExcel(divisionId, academicYearId, Semester.valueOf(semester));
            return ResponseEntity.ok()
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .header("Content-Disposition", "attachment; filename=timetable_division_" + divisionId + "_" + semester + ".xlsx")
                .body(excel);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error generating division Excel: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/export/teacher/{teacherId}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'HOD', 'TIMETABLE_COORDINATOR', 'TEACHER')")
    public ResponseEntity<byte[]> exportTeacherPDF(
        @PathVariable Long teacherId,
        @RequestParam Long academicYearId
    ) {
        try {
            authService.checkTeacherAccess(teacherId);
            byte[] pdf = exportService.generateTeacherPDF(teacherId, academicYearId);
            return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=timetable_teacher_" + teacherId + ".pdf")
                .body(pdf);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/export/teacher/{teacherId}/excel")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'HOD', 'TIMETABLE_COORDINATOR', 'TEACHER')")
    public ResponseEntity<byte[]> exportTeacherExcel(
        @PathVariable Long teacherId,
        @RequestParam Long academicYearId
    ) {
        try {
            authService.checkTeacherAccess(teacherId);
            byte[] excel = exportService.generateTeacherExcel(teacherId, academicYearId);
            return ResponseEntity.ok()
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .header("Content-Disposition", "attachment; filename=timetable_teacher_" + teacherId + ".xlsx")
                .body(excel);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/export/department/{departmentId}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'HOD', 'TIMETABLE_COORDINATOR')")
    public ResponseEntity<byte[]> exportDepartmentPDF(
        @PathVariable Long departmentId,
        @RequestParam Long academicYearId,
        @RequestParam String semester
    ) {
        try {
            authService.checkDepartmentAccess(departmentId);
            byte[] pdf = exportService.generateDepartmentPDF(departmentId, academicYearId, Semester.valueOf(semester));
            return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=timetable_department_" + departmentId + "_" + semester + ".pdf")
                .body(pdf);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/export/department/{departmentId}/excel")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'HOD', 'TIMETABLE_COORDINATOR')")
    public ResponseEntity<byte[]> exportDepartmentExcel(
        @PathVariable Long departmentId,
        @RequestParam Long academicYearId,
        @RequestParam String semester
    ) {
        try {
            authService.checkDepartmentAccess(departmentId);
            byte[] excel = exportService.generateDepartmentExcel(departmentId, academicYearId, Semester.valueOf(semester));
            return ResponseEntity.ok()
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .header("Content-Disposition", "attachment; filename=timetable_department_" + departmentId + "_" + semester + ".xlsx")
                .body(excel);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/export/room/{roomId}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'HOD', 'TIMETABLE_COORDINATOR')")
    public ResponseEntity<byte[]> exportRoomPDF(
        @PathVariable Long roomId,
        @RequestParam Long academicYearId,
        @RequestParam String semester
    ) {
        try {
            authService.checkRoomAccess(roomId);
            byte[] pdf = exportService.generateRoomPDF(roomId, academicYearId, Semester.valueOf(semester));
            return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=timetable_room_" + roomId + "_" + semester + ".pdf")
                .body(pdf);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/export/room/{roomId}/excel")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'HOD', 'TIMETABLE_COORDINATOR')")
    public ResponseEntity<byte[]> exportRoomExcel(
        @PathVariable Long roomId,
        @RequestParam Long academicYearId,
        @RequestParam String semester
    ) {
        try {
            authService.checkRoomAccess(roomId);
            byte[] excel = exportService.generateRoomExcel(roomId, academicYearId, Semester.valueOf(semester));
            return ResponseEntity.ok()
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .header("Content-Disposition", "attachment; filename=timetable_room_" + roomId + "_" + semester + ".xlsx")
                .body(excel);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // ---------------------------------------------------------------
    // READ endpoints — available to all authenticated users
    // ---------------------------------------------------------------

    /**
     * GET /api/timetable/division/{divisionId}?academicYearId=1
     * Get PUBLISHED timetable for a division (students + teachers view)
     * Served from Redis cache
     */
    @GetMapping("/division/{divisionId}")
    public ResponseEntity<List<TimetableEntry>> getDivisionTimetable(
        @PathVariable Long divisionId,
        @RequestParam Long academicYearId
    ) {
        return ResponseEntity.ok(
            timetableService.getDivisionTimetable(divisionId, academicYearId)
        );
    }

    /**
     * GET /api/timetable/teacher/{teacherId}?academicYearId=1
     * Get PUBLISHED timetable for a specific teacher
     * Served from Redis cache
     */
    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<List<TimetableEntry>> getTeacherTimetable(
        @PathVariable Long teacherId,
        @RequestParam Long academicYearId
    ) {
        return ResponseEntity.ok(
            timetableService.getTeacherTimetable(teacherId, academicYearId)
        );
    }

    /**
     * GET /api/timetable/department/{departmentId}?academicYearId=1
     * Get PUBLISHED timetable for an entire department (faculty read-only view)
     * Served from Redis cache
     */
    @GetMapping("/department/{departmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'HOD', 'TIMETABLE_COORDINATOR', 'TEACHER')")
    public ResponseEntity<List<TimetableEntry>> getDepartmentTimetable(
        @PathVariable Long departmentId,
        @RequestParam Long academicYearId
    ) {
        authService.checkDepartmentAccess(departmentId);
        return ResponseEntity.ok(
            timetableService.getDepartmentTimetable(departmentId, academicYearId)
        );
    }

    /**
     * GET /api/timetable/room/{roomId}?academicYearId=1
     * Get published and draft timetable entries for a room (weekly occupancy view)
     */
    @GetMapping("/room/{roomId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'HOD', 'TIMETABLE_COORDINATOR', 'TEACHER')")
    public ResponseEntity<List<TimetableEntry>> getRoomTimetable(
        @PathVariable Long roomId,
        @RequestParam Long academicYearId
    ) {
        authService.checkRoomAccess(roomId);
        return ResponseEntity.ok(
            timetableService.getRoomTimetable(roomId, academicYearId)
        );
    }

    /**
     * GET /api/timetable/draft?divisionId=1&academicYearId=1&semester=SEM_3
     * Get DRAFT timetable for admin review (not cached)
     * Filters by semester to show only entries for that specific semester
     */
    @GetMapping("/draft")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TimetableEntry>> getDraftTimetable(
        @RequestParam Long divisionId,
        @RequestParam Long academicYearId,
        @RequestParam String semester
    ) {
        return ResponseEntity.ok(
            timetableService.getDraftTimetable(divisionId, academicYearId, Semester.valueOf(semester))
        );
    }

    /**
     * GET /api/timetable/editable
     * Get DRAFT and PUBLISHED timetable entries for admin editing
     * This allows admins to continue editing even after publishing
     * Filters by semester to show only entries for that specific semester
     */
    @GetMapping("/editable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TimetableEntry>> getEditableTimetable(
        @RequestParam Long divisionId,
        @RequestParam Long academicYearId,
        @RequestParam String semester
    ) {
        return ResponseEntity.ok(
            timetableService.getEditableTimetable(divisionId, academicYearId, Semester.valueOf(semester))
        );
    }

    // ---------------------------------------------------------------
    // WRITE endpoints — admin only
    // ---------------------------------------------------------------

    /**
     * POST /api/timetable/entries
     * Add a single timetable entry (manual mode)
     * Conflict check runs before saving — returns 409 if conflict found
     */
    @PostMapping("/entries")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'HOD', 'TIMETABLE_COORDINATOR')")
    public ResponseEntity<?> addEntry(@Valid @RequestBody CreateTimetableEntryDTO dto) {
        authService.checkDivisionAccess(dto.getDivisionId());
        // Debug logging
        System.out.println("DEBUG: Received DTO - batchId: " + dto.getBatchId() + ", labSessionGroupId: " + dto.getLabSessionGroupId());
        
        try {
            TimetableEntry entry = timetableService.addEntry(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(entry);
        } catch (TimetableConflictException e) {
            // Return ALL conflicts at once — admin sees everything, not just the first error
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                Map.of(
                    "message", "Scheduling conflicts detected",
                    "conflicts", e.getConflicts()
                )
            );
        }
    }

    /**
     * PUT /api/timetable/entries/{id}
     * Update an existing DRAFT entry
     */
    @PutMapping("/entries/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'HOD', 'TIMETABLE_COORDINATOR')")
    public ResponseEntity<?> updateEntry(
        @PathVariable Long id,
        @Valid @RequestBody CreateTimetableEntryDTO dto
    ) {
        authService.checkTimetableEntryAccess(id);
        authService.checkDivisionAccess(dto.getDivisionId());
        try {
            TimetableEntry entry = timetableService.updateEntry(id, dto);
            return ResponseEntity.ok(entry);
        } catch (TimetableConflictException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                Map.of(
                    "message", "Scheduling conflicts detected",
                    "conflicts", e.getConflicts()
                )
            );
        }
    }

    /**
     * DELETE /api/timetable/entries/{id}
     * Delete a DRAFT entry
     */
    @DeleteMapping("/entries/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'HOD', 'TIMETABLE_COORDINATOR')")
    public ResponseEntity<Map<String, String>> deleteEntry(@PathVariable Long id) {
        authService.checkTimetableEntryAccess(id);
        timetableService.deleteEntry(id);
        return ResponseEntity.ok(Map.of("message", "Entry deleted successfully"));
    }

    /**
     * POST /api/timetable/publish?divisionId=1&academicYearId=1
     * Publish all DRAFT entries for a division — makes timetable live
     * Clears Redis cache automatically
     */
    /**
     * GET /api/timetable/validate?divisionId=1&academicYearId=2
     * Run pre-publish validation — returns errors (blocking) and warnings (informational).
     */
    @GetMapping("/validate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'HOD', 'TIMETABLE_COORDINATOR')")
    public ResponseEntity<?> validateTimetable(
        @RequestParam Long divisionId,
        @RequestParam Long academicYearId
    ) {
        authService.checkDivisionAccess(divisionId);
        var result = validationService.validate(divisionId, academicYearId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/publish")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'HOD', 'TIMETABLE_COORDINATOR')")
    public ResponseEntity<?> publishTimetable(
        @RequestParam Long divisionId,
        @RequestParam Long academicYearId,
        @RequestParam String semester,
        @RequestParam(defaultValue = "false") boolean force
    ) {
        authService.checkDivisionAccess(divisionId);
        // Run validation before publishing (validation will be updated to be semester-aware)
        var validation = validationService.validate(divisionId, academicYearId);

        if (validation.hasErrors() && !force) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "message", "Timetable has validation errors that must be fixed before publishing",
                "publishable", false,
                "errors", validation.getErrors(),
                "warnings", validation.getWarnings()
            ));
        }

        int count = timetableService.publishTimetable(divisionId, academicYearId, Semester.valueOf(semester));
        return ResponseEntity.ok(Map.of(
            "message", "Timetable published successfully for " + semester,
            "entriesPublished", count,
            "warnings", validation.getWarnings()
        ));
    }

    /**
     * POST /api/timetable/archive?divisionId=1&academicYearId=1&semester=SEM_3
     * Archive current published timetable (end of semester)
     */
    @PostMapping("/archive")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'HOD', 'TIMETABLE_COORDINATOR')")
    public ResponseEntity<Map<String, Object>> archiveTimetable(
        @RequestParam Long divisionId,
        @RequestParam Long academicYearId,
        @RequestParam String semester
    ) {
        authService.checkDivisionAccess(divisionId);
        int count = timetableService.archiveTimetable(divisionId, academicYearId, Semester.valueOf(semester));
        return ResponseEntity.ok(Map.of(
            "message", "Timetable archived successfully for " + semester,
            "entriesArchived", count
        ));
    }

    /**
     * DELETE /api/timetable/draft?divisionId=1&academicYearId=1&semester=SEM_3
     * Clear all DRAFT entries for a semester — start fresh
     */
    @DeleteMapping("/draft")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'HOD', 'TIMETABLE_COORDINATOR')")
    public ResponseEntity<Map<String, Object>> clearDraft(
        @RequestParam Long divisionId,
        @RequestParam Long academicYearId,
        @RequestParam String semester
    ) {
        authService.checkDivisionAccess(divisionId);
        int count = timetableService.clearDraft(divisionId, academicYearId, Semester.valueOf(semester));
        return ResponseEntity.ok(Map.of(
            "message", "Draft cleared for " + semester,
            "entriesDeleted", count
        ));
    }

    // ---------------------------------------------------------------
    // LAB SESSION GROUP endpoints
    // ---------------------------------------------------------------

    /**
     * POST /api/timetable/lab-groups
     * Create a lab session group FIRST, then add batch entries pointing to it
     */
    @PostMapping("/lab-groups")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'HOD', 'TIMETABLE_COORDINATOR')")
    public ResponseEntity<LabSessionGroup> createLabSessionGroup(
        @Valid @RequestBody CreateLabSessionGroupDTO dto
    ) {
        authService.checkDivisionAccess(dto.getDivisionId());
        LabSessionGroup group = timetableService.createLabSessionGroup(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(group);
    }

    /**
     * POST /api/timetable/lab-session
     * Single-step lab session creation — creates group + all batch entries at once.
     */
    @PostMapping("/lab-session")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'HOD', 'TIMETABLE_COORDINATOR')")
    public ResponseEntity<?> createLabSession(
        @Valid @RequestBody com.College.timetable.IO.CreateLabSessionRequest request
    ) {
        authService.checkDivisionAccess(request.getDivisionId());
        try {
            Map<String, Object> result = timetableService.createLabSession(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (TimetableConflictException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                Map.of("message", "Lab session conflicts detected", "conflicts", e.getConflicts())
            );
        }
    }

    /**
     * DELETE /api/timetable/lab-groups/{groupId}
     * Delete entire lab session group and all its entries.
     */
    @DeleteMapping("/lab-groups/{groupId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'HOD', 'TIMETABLE_COORDINATOR')")
    public ResponseEntity<Map<String, Object>> deleteLabGroup(@PathVariable Long groupId) {
        authService.checkLabSessionGroupAccess(groupId);
        int deleted = timetableService.deleteLabSessionGroup(groupId);
        return ResponseEntity.ok(Map.of(
            "message", "Lab session group deleted",
            "entriesDeleted", deleted
        ));
    }

    /**
     * Copy draft timetable from one division to another (same academic year).
     * Only copies non-lab entries. Lab sessions must be configured separately.
     */
    @PostMapping("/copy")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'HOD', 'TIMETABLE_COORDINATOR')")
    public ResponseEntity<Map<String, Object>> copyTimetable(
        @RequestParam Long sourceDivisionId,
        @RequestParam Long targetDivisionId,
        @RequestParam Long academicYearId
    ) {
        authService.checkDivisionAccess(sourceDivisionId);
        authService.checkDivisionAccess(targetDivisionId);
        int copied = timetableService.copyDraftEntries(sourceDivisionId, targetDivisionId, academicYearId);
        return ResponseEntity.ok(Map.of(
            "message", "Timetable copied successfully",
            "entriesCopied", copied,
            "note", "Lab session entries were skipped — configure them separately for the target division"
        ));
    }

    // ---------------------------------------------------------------
    // AVAILABILITY FILTERING — Real-time filtering for timetable creation
    // ---------------------------------------------------------------

    /**
     * GET /api/timetable/available-rooms?day=MONDAY&slotId=5&academicYearId=1&divisionId=1&semester=SEM_3&courseId=1&batchId=2
     * Get available rooms for a specific day + time slot with smart filtering.
     */
    @GetMapping("/available-rooms")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'TIMETABLE_COORDINATOR')")
    public ResponseEntity<List<com.College.timetable.Entity.ClassRoom>> getAvailableRooms(
        @RequestParam com.College.timetable.Entity.DayOfWeek day,
        @RequestParam Long slotId,
        @RequestParam Long academicYearId,
        @RequestParam(required = false) Long divisionId,
        @RequestParam String semester,
        @RequestParam(required = false) Long courseId,
        @RequestParam(required = false) Long batchId
    ) {
        List<com.College.timetable.Entity.ClassRoom> available = timetableService.getAvailableRooms(
            day, slotId, academicYearId, divisionId, com.College.timetable.Entity.Semester.valueOf(semester), courseId, batchId
        );
        return ResponseEntity.ok(available);
    }

    /**
     * GET /api/timetable/available-teachers?day=MONDAY&slotId=5&academicYearId=1&semester=SEM_3&courseId=1
     * Get available teachers for a specific day + time slot with smart workload filtering.
     */
    @GetMapping("/available-teachers")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'TIMETABLE_COORDINATOR')")
    public ResponseEntity<List<com.College.timetable.Entity.TeacherEntity>> getAvailableTeachers(
        @RequestParam com.College.timetable.Entity.DayOfWeek day,
        @RequestParam Long slotId,
        @RequestParam Long academicYearId,
        @RequestParam String semester,
        @RequestParam(required = false) Long courseId
    ) {
        List<com.College.timetable.Entity.TeacherEntity> available = timetableService.getAvailableTeachers(
            day, slotId, academicYearId, com.College.timetable.Entity.Semester.valueOf(semester), courseId
        );
        return ResponseEntity.ok(available);
    }

    /**
     * GET /api/timetable/available-batches?divisionId=1&day=MONDAY&slotId=5&academicYearId=1&semester=SEM_3
     * Get available batches for a specific division, day + time slot.
     */
    @GetMapping("/available-batches")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'TIMETABLE_COORDINATOR')")
    public ResponseEntity<List<com.College.timetable.Entity.Batch>> getAvailableBatches(
        @RequestParam Long divisionId,
        @RequestParam com.College.timetable.Entity.DayOfWeek day,
        @RequestParam Long slotId,
        @RequestParam Long academicYearId,
        @RequestParam String semester
    ) {
        List<com.College.timetable.Entity.Batch> available = timetableService.getAvailableBatches(
            divisionId, day, slotId, academicYearId, com.College.timetable.Entity.Semester.valueOf(semester)
        );
        return ResponseEntity.ok(available);
    }

    /**
     * GET /api/timetable/analytics?academicYearId=1
     * Get aggregated teacher workload, room utilization, and slot density statistics.
     */
    @GetMapping("/analytics")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'HOD', 'TIMETABLE_COORDINATOR')")
    public ResponseEntity<Map<String, Object>> getAnalytics(@RequestParam Long academicYearId) {
        var teacherWorkloads = timetableService.getTeacherWorkloads(academicYearId);
        var roomUtilizations = timetableService.getRoomUtilizations(academicYearId);
        var slotDensity = timetableService.getSlotDensity(academicYearId);

        return ResponseEntity.ok(Map.of(
            "teacherWorkloads", teacherWorkloads,
            "roomBookingCounts", roomUtilizations,
            "slotDensity", slotDensity
        ));
    }

    /**
     * GET /api/timetable/dashboard-stats?academicYearId=1
     * Get unified dashboard statistics for counts, room occupancy utilization, and recent timetable changes.
     */
    @GetMapping("/dashboard-stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'HOD', 'TIMETABLE_COORDINATOR')")
    public ResponseEntity<Map<String, Object>> getDashboardStats(
        @RequestParam Long academicYearId,
        @RequestParam(required = false) Long departmentId
    ) {
        if (!authService.isInstitutionalAdmin()) {
            Long userDeptId = authService.getCurrentUser().getDepartment().getId();
            return ResponseEntity.ok(timetableService.getDashboardStats(userDeptId, academicYearId));
        }
        return ResponseEntity.ok(timetableService.getDashboardStats(departmentId, academicYearId));
    }
}
