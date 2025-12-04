package com.College.timetable.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.College.timetable.Entity.TeacherEntity;
import com.College.timetable.IO.ProfileUpdateRequest;
import com.College.timetable.Service.TeacherService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/teachers")
public class TeacherController {

	@Autowired
	private TeacherService teacherService;
	
	@PostMapping
	public ResponseEntity<TeacherEntity> addTeacher(@Valid @RequestBody TeacherEntity teach) {
		TeacherEntity saved = teacherService.add(teach);
		return ResponseEntity.ok(saved);
	}
	
	@GetMapping
	public ResponseEntity<List<TeacherEntity>> getAllTeachers() {
		return ResponseEntity.ok(teacherService.getAll());
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<TeacherEntity> getTeacherById(@PathVariable Long id) {
		return ResponseEntity.ok(teacherService.getById(id));
	}
	
	@GetMapping("/profile")
	public ResponseEntity<TeacherEntity> getProfile(Authentication authentication) {
		String email = authentication.getName();
		return ResponseEntity.ok(teacherService.getByEmail(email));
	}
	
	@PutMapping("/profile")
	public ResponseEntity<TeacherEntity> updateProfile(Authentication authentication, @Valid @RequestBody ProfileUpdateRequest request) {
		String email = authentication.getName();
		TeacherEntity currentTeacher = teacherService.getByEmail(email);
		TeacherEntity updated = teacherService.updateProfile(currentTeacher.getId(), request);
		return ResponseEntity.ok(updated);
	}
	
	@PutMapping("/{id}")
	public ResponseEntity<TeacherEntity> updateTeacher(@PathVariable Long id, @Valid @RequestBody TeacherEntity teach) {
		TeacherEntity updated = teacherService.update(id, teach);
		return ResponseEntity.ok(updated);
	}
	
	@DeleteMapping("/{id}")
	public ResponseEntity<String> deleteTeacher(@PathVariable Long id) {
		teacherService.delete(id);
		return ResponseEntity.ok("Teacher deleted successfully");
	}
	
	// Admin approval endpoints
	@GetMapping("/pending-approvals")
	public ResponseEntity<List<TeacherEntity>> getPendingApprovals() {
		return ResponseEntity.ok(teacherService.getPendingApprovals());
	}
	
	@PostMapping("/{id}/approve")
	public ResponseEntity<TeacherEntity> approveTeacher(@PathVariable Long id) {
		TeacherEntity approved = teacherService.approveTeacher(id);
		return ResponseEntity.ok(approved);
	}
	
	@PostMapping("/{id}/reject")
	public ResponseEntity<TeacherEntity> rejectTeacher(@PathVariable Long id, @RequestBody(required = false) String reason) {
		String rejectionReason = (reason != null && !reason.isEmpty()) ? reason : "Application rejected by administrator";
		TeacherEntity rejected = teacherService.rejectTeacher(id, rejectionReason);
		return ResponseEntity.ok(rejected);
	}
}
