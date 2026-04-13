package com.College.timetable.Controller;

import com.College.timetable.Entity.LabSessionGroup;
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

    // ---------------------------------------------------------------
    // EXPORT endpoints — PDF & Excel download
    // ---------------------------------------------------------------

    @GetMapping("/export/division/{divisionId}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'TIMETABLE_COORDINATOR')")
    public ResponseEntity<byte[]> exportDivisionPDF(
        @PathVariable Long divisionId,
        @RequestParam Long academicYearId
    ) {
        try {
            byte[] pdf = exportService.generateDivisionPDF(divisionId, academicYearId);
            return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=timetable_division_" + divisionId + ".pdf")
                .body(pdf);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/export/division/{divisionId}/excel")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'TIMETABLE_COORDINATOR')")
    public ResponseEntity<byte[]> exportDivisionExcel(
        @PathVariable Long divisionId,
        @RequestParam Long academicYearId
    ) {
        try {
            byte[] excel = exportService.generateDivisionExcel(divisionId, academicYearId);
            return ResponseEntity.ok()
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .header("Content-Disposition", "attachment; filename=timetable_division_" + divisionId + ".xlsx")
                .body(excel);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/export/teacher/{teacherId}/pdf")
    public ResponseEntity<byte[]> exportTeacherPDF(
        @PathVariable Long teacherId,
        @RequestParam Long academicYearId
    ) {
        try {
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
    public ResponseEntity<byte[]> exportTeacherExcel(
        @PathVariable Long teacherId,
        @RequestParam Long academicYearId
    ) {
        try {
            byte[] excel = exportService.generateTeacherExcel(teacherId, academicYearId);
            return ResponseEntity.ok()
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .header("Content-Disposition", "attachment; filename=timetable_teacher_" + teacherId + ".xlsx")
                .body(excel);
        } catch (Exception e) {
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
     * GET /api/timetable/draft?divisionId=1&academicYearId=1
     * Get DRAFT timetable for admin review (not cached)
     */
    @GetMapping("/draft")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TimetableEntry>> getDraftTimetable(
        @RequestParam Long divisionId,
        @RequestParam Long academicYearId
    ) {
        return ResponseEntity.ok(
            timetableService.getDraftTimetable(divisionId, academicYearId)
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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addEntry(@Valid @RequestBody CreateTimetableEntryDTO dto) {
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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateEntry(
        @PathVariable Long id,
        @Valid @RequestBody CreateTimetableEntryDTO dto
    ) {
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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteEntry(@PathVariable Long id) {
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
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'TIMETABLE_COORDINATOR')")
    public ResponseEntity<?> validateTimetable(
        @RequestParam Long divisionId,
        @RequestParam Long academicYearId
    ) {
        var result = validationService.validate(divisionId, academicYearId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> publishTimetable(
        @RequestParam Long divisionId,
        @RequestParam Long academicYearId,
        @RequestParam(defaultValue = "false") boolean force
    ) {
        // Run validation before publishing
        var validation = validationService.validate(divisionId, academicYearId);

        if (validation.hasErrors() && !force) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "message", "Timetable has validation errors that must be fixed before publishing",
                "publishable", false,
                "errors", validation.getErrors(),
                "warnings", validation.getWarnings()
            ));
        }

        int count = timetableService.publishTimetable(divisionId, academicYearId);
        return ResponseEntity.ok(Map.of(
            "message", "Timetable published successfully",
            "entriesPublished", count,
            "warnings", validation.getWarnings()
        ));
    }

    /**
     * POST /api/timetable/archive?divisionId=1&academicYearId=1
     * Archive current published timetable (end of semester)
     */
    @PostMapping("/archive")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> archiveTimetable(
        @RequestParam Long divisionId,
        @RequestParam Long academicYearId
    ) {
        int count = timetableService.archiveTimetable(divisionId, academicYearId);
        return ResponseEntity.ok(Map.of(
            "message", "Timetable archived successfully",
            "entriesArchived", count
        ));
    }

    /**
     * DELETE /api/timetable/draft?divisionId=1&academicYearId=1
     * Clear all DRAFT entries — start fresh
     */
    @DeleteMapping("/draft")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> clearDraft(
        @RequestParam Long divisionId,
        @RequestParam Long academicYearId
    ) {
        int count = timetableService.clearDraft(divisionId, academicYearId);
        return ResponseEntity.ok(Map.of(
            "message", "Draft cleared",
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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LabSessionGroup> createLabSessionGroup(
        @Valid @RequestBody CreateLabSessionGroupDTO dto
    ) {
        LabSessionGroup group = timetableService.createLabSessionGroup(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(group);
    }

    /**
     * POST /api/timetable/lab-session
     * Single-step lab session creation — creates group + all batch entries at once.
     */
    @PostMapping("/lab-session")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'TIMETABLE_COORDINATOR')")
    public ResponseEntity<?> createLabSession(
        @Valid @RequestBody com.College.timetable.IO.CreateLabSessionRequest request
    ) {
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
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'TIMETABLE_COORDINATOR')")
    public ResponseEntity<Map<String, Object>> deleteLabGroup(@PathVariable Long groupId) {
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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> copyTimetable(
        @RequestParam Long sourceDivisionId,
        @RequestParam Long targetDivisionId,
        @RequestParam Long academicYearId
    ) {
        int copied = timetableService.copyDraftEntries(sourceDivisionId, targetDivisionId, academicYearId);
        return ResponseEntity.ok(Map.of(
            "message", "Timetable copied successfully",
            "entriesCopied", copied,
            "note", "Lab session entries were skipped — configure them separately for the target division"
        ));
    }
}
