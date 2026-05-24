package com.College.timetable.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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

import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin/api/courses")
public class CourseController {
	
	@Autowired
	private CourseService courseService;
	
	@PostMapping
	public ResponseEntity<CourseEntity> addCourse(@Valid @RequestBody CourseEntity cor) {
		CourseEntity saved = courseService.add(cor);
		return ResponseEntity.ok(saved);
	}
	
	@GetMapping
	public ResponseEntity<List<CourseEntity>> getAllCourses() {
		return ResponseEntity.ok(courseService.getAll());
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<CourseEntity> getCourseById(@PathVariable Long id) {
		return ResponseEntity.ok(courseService.getById(id));
	}
	
	@PutMapping("/{id}")
	public ResponseEntity<CourseEntity> updateCourse(@PathVariable Long id, @Valid @RequestBody CourseEntity cor) {
		CourseEntity updated = courseService.update(id, cor);
		return ResponseEntity.ok(updated);
	}
	
	@DeleteMapping("/{id}")
	public ResponseEntity<String> deleteCourse(@PathVariable Long id) {
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
		List<java.util.Map<String, Object>> available = courseService.getAvailableBatchesForCourse(
			courseId, divisionId, academicYearId, semester != null ? Semester.valueOf(semester) : null
		);
		return ResponseEntity.ok(available);
	}
}
