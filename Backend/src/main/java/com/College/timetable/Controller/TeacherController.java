package com.College.timetable.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import com.College.timetable.Service.DepartmentAuthorizationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/teachers")
public class TeacherController {

	@Autowired
	private TeacherService teacherService;

	@Autowired
	private DepartmentAuthorizationService authService;
	
	@PostMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'HOD')")
	public ResponseEntity<TeacherEntity> addTeacher(@Valid @RequestBody TeacherEntity teach) {
		if (teach.getDepartment() != null && teach.getDepartment().getId() != null) {
			authService.checkDepartmentAccess(teach.getDepartment().getId());
		} else {
			authService.checkSuperAdminAccess();
		}
		TeacherEntity saved = teacherService.add(teach);
		return ResponseEntity.ok(saved);
	}
	
	@GetMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'HOD', 'TIMETABLE_COORDINATOR')")
	public ResponseEntity<List<TeacherEntity>> getAllTeachers() {
		List<TeacherEntity> all = teacherService.getAll();
		if (authService.isInstitutionalAdmin()) {
			return ResponseEntity.ok(all);
		}
		Long deptId = authService.getCurrentUser().getDepartment().getId();
		List<TeacherEntity> filtered = all.stream()
			.filter(t -> t.getDepartment() != null && deptId.equals(t.getDepartment().getId()))
			.toList();
		return ResponseEntity.ok(filtered);
	}
	
	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'HOD', 'TIMETABLE_COORDINATOR')")
	public ResponseEntity<TeacherEntity> getTeacherById(@PathVariable Long id) {
		authService.checkTeacherAccess(id);
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
	@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'HOD')")
	public ResponseEntity<TeacherEntity> updateTeacher(@PathVariable Long id, @Valid @RequestBody TeacherEntity teach) {
		authService.checkTeacherAccess(id);
		if (teach.getDepartment() != null && teach.getDepartment().getId() != null) {
			authService.checkDepartmentAccess(teach.getDepartment().getId());
		}
		TeacherEntity updated = teacherService.update(id, teach);
		return ResponseEntity.ok(updated);
	}
	
	@DeleteMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'HOD')")
	public ResponseEntity<String> deleteTeacher(@PathVariable Long id) {
		authService.checkTeacherAccess(id);
		teacherService.delete(id);
		return ResponseEntity.ok("Teacher deleted successfully");
	}
	
	// Admin approval endpoints
	@GetMapping("/pending-approvals")
	@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'HOD')")
	public ResponseEntity<List<TeacherEntity>> getPendingApprovals() {
		List<TeacherEntity> all = teacherService.getPendingApprovals();
		if (authService.isInstitutionalAdmin()) {
			return ResponseEntity.ok(all);
		}
		Long deptId = authService.getCurrentUser().getDepartment().getId();
		List<TeacherEntity> filtered = all.stream()
			.filter(t -> t.getDepartment() != null && deptId.equals(t.getDepartment().getId()))
			.toList();
		return ResponseEntity.ok(filtered);
	}
	
	@PostMapping("/{id}/approve")
	@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'HOD')")
	public ResponseEntity<TeacherEntity> approveTeacher(@PathVariable Long id) {
		authService.checkTeacherAccess(id);
		TeacherEntity approved = teacherService.approveTeacher(id);
		return ResponseEntity.ok(approved);
	}
	
	@PostMapping("/{id}/reject")
	@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'HOD')")
	public ResponseEntity<TeacherEntity> rejectTeacher(@PathVariable Long id, @RequestBody(required = false) String reason) {
		authService.checkTeacherAccess(id);
		String rejectionReason = (reason != null && !reason.isEmpty()) ? reason : "Application rejected by administrator";
		TeacherEntity rejected = teacherService.rejectTeacher(id, rejectionReason);
		return ResponseEntity.ok(rejected);
	}
}
