package com.College.timetable.Controller;

import com.College.timetable.Entity.TeacherEntity;
import com.College.timetable.Entity.TimetableEntry;
import com.College.timetable.Entity.Semester;
import com.College.timetable.Service.TimetableService;
import com.College.timetable.Service.DepartmentAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/faculty")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'HOD', 'TIMETABLE_COORDINATOR', 'TEACHER')")
public class FacultyTimetableController {

    private final TimetableService timetableService;
    private final DepartmentAuthorizationService authService;

    @GetMapping("/my-timetable")
    public ResponseEntity<List<TimetableEntry>> getMyTimetable(
        @RequestParam Long academicYearId
    ) {
        TeacherEntity currentTeacher = authService.getCurrentUser();
        List<TimetableEntry> entries = timetableService.getTeacherTimetable(currentTeacher.getId(), academicYearId);
        return ResponseEntity.ok(entries);
    }

    @GetMapping("/department-timetable")
    public ResponseEntity<List<TimetableEntry>> getDepartmentTimetable(
        @RequestParam Long academicYearId,
        @RequestParam(required = false) String semester
    ) {
        TeacherEntity currentTeacher = authService.getCurrentUser();
        if (currentTeacher.getDepartment() == null) {
            return ResponseEntity.badRequest().build();
        }
        Long departmentId = currentTeacher.getDepartment().getId();
        List<TimetableEntry> entries = timetableService.getDepartmentTimetable(departmentId, academicYearId);
        if (semester != null && !semester.isEmpty()) {
            Semester semEnum = Semester.valueOf(semester);
            entries = entries.stream()
                .filter(e -> semEnum.equals(e.getSemester()))
                .toList();
        }
        return ResponseEntity.ok(entries);
    }
}
