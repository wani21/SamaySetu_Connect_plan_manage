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
import org.springframework.web.bind.annotation.RestController;

import com.College.timetable.Entity.CourseEntity;
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
}
