package com.College.timetable.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.College.timetable.Entity.CourseEntity;
import com.College.timetable.Entity.Semester;
import com.College.timetable.Service.CourseService;
import com.College.timetable.Service.DepartmentAuthorizationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin/api/courses")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'HOD', 'TIMETABLE_COORDINATOR')")
public class CourseController {
	
	@Autowired
	private CourseService courseService;

	@Autowired
	private DepartmentAuthorizationService authService;
	
	@PostMapping
	public ResponseEntity<CourseEntity> addCourse(@Valid @RequestBody CourseEntity cor) {
		if (cor.getDepartment() != null && cor.getDepartment().getId() != null) {
			authService.checkDepartmentAccess(cor.getDepartment().getId());
		} else {
			authService.checkSuperAdminAccess();
		}
		CourseEntity saved = courseService.add(cor);
		return ResponseEntity.ok(saved);
	}
	
	@GetMapping
	public ResponseEntity<List<CourseEntity>> getAllCourses() {
		List<CourseEntity> all = courseService.getAll();
		if (authService.isInstitutionalAdmin()) {
			return ResponseEntity.ok(all);
		}
		Long deptId = authService.getCurrentUser().getDepartment().getId();
		List<CourseEntity> filtered = all.stream()
			.filter(c -> c.getDepartment() != null && deptId.equals(c.getDepartment().getId()))
			.toList();
		return ResponseEntity.ok(filtered);
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<CourseEntity> getCourseById(@PathVariable Long id) {
		authService.checkCourseAccess(id);
		return ResponseEntity.ok(courseService.getById(id));
	}
	
	@PutMapping("/{id}")
	public ResponseEntity<CourseEntity> updateCourse(@PathVariable Long id, @Valid @RequestBody CourseEntity cor) {
		authService.checkCourseAccess(id);
		if (cor.getDepartment() != null && cor.getDepartment().getId() != null) {
			authService.checkDepartmentAccess(cor.getDepartment().getId());
		}
		CourseEntity updated = courseService.update(id, cor);
		return ResponseEntity.ok(updated);
	}
	
	@DeleteMapping("/{id}")
	public ResponseEntity<String> deleteCourse(@PathVariable Long id) {
		authService.checkCourseAccess(id);
		courseService.delete(id);
		return ResponseEntity.ok("Course deleted successfully");
	}
	
	/**
	 * GET /admin/api/courses/available?divisionId=1&academicYearId=1&semester=SEM_3
	 * Get available courses with progressive instances for a division.
	 * Returns courses with their next available instance number based on hours_per_week.
	 * 
	 * Logic:
	 * - Theory: hours_per_week = instances (e.g., 3 hours = 3 instances)
	 * - Lab: hours_per_week / 2 = instances (e.g., 4 hours = 2 instances)
	 * Example: If "OS" has 3 hours_per_week and 1 entry exists, returns "OS (2)"
	 */
	@GetMapping("/available")
	public ResponseEntity<List<java.util.Map<String, Object>>> getAvailableCoursesWithCredits(
		@RequestParam Long divisionId,
		@RequestParam Long academicYearId,
		@RequestParam(required = false) String semester
	) {
		authService.checkDivisionAccess(divisionId);
		List<java.util.Map<String, Object>> available = courseService.getAvailableCoursesWithCredits(
			divisionId, academicYearId, semester != null ? Semester.valueOf(semester) : null
		);
		return ResponseEntity.ok(available);
	}
	
	/**
	 * GET /admin/api/courses/{courseId}/available-batches?divisionId=1&academicYearId=1&semester=SEM_3
	 * Get available batches for a specific lab course.
	 * Returns batches that have NOT been allocated this lab course yet FOR THIS SEMESTER.
	 * Used for dynamic batch filtering when a lab course is selected.
	 */
	@GetMapping("/{courseId}/available-batches")
	public ResponseEntity<List<java.util.Map<String, Object>>> getAvailableBatchesForCourse(
		@PathVariable Long courseId,
		@RequestParam Long divisionId,
		@RequestParam Long academicYearId,
		@RequestParam(required = false) String semester
	) {
		authService.checkCourseAccess(courseId);
		authService.checkDivisionAccess(divisionId);
		List<java.util.Map<String, Object>> available = courseService.getAvailableBatchesForCourse(
			courseId, divisionId, academicYearId, semester != null ? Semester.valueOf(semester) : null
		);
		return ResponseEntity.ok(available);
	}
	
	/**
	 * GET /admin/api/courses/check-short-name?shortName=DS&departmentId=1&year=3&excludeId=5
	 * Check if a course short name is available for a specific department and year.
	 * Returns availability status and suggestions if taken.
	 * 
	 * @param shortName The short name to check (case-insensitive)
	 * @param departmentId The department ID
	 * @param year The academic year (1-4)
	 * @param excludeId Optional course ID to exclude from check (for updates)
	 * @return Map with availability, message, and suggestions
	 */
	@GetMapping("/check-short-name")
	public ResponseEntity<java.util.Map<String, Object>> checkShortName(
		@RequestParam String shortName,
		@RequestParam Long departmentId,
		@RequestParam Integer year,
		@RequestParam(required = false) Long excludeId
	) {
		authService.checkDepartmentAccess(departmentId);
		java.util.Map<String, Object> result = courseService.checkShortNameAvailability(
			shortName, departmentId, year, excludeId
		);
		return ResponseEntity.ok(result);
	}
}
