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
import org.springframework.web.bind.annotation.RestController;

import com.College.timetable.Entity.Division;
import com.College.timetable.Service.DivisionService;
import com.College.timetable.Service.DepartmentAuthorizationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin/api/divisions")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'HOD', 'TIMETABLE_COORDINATOR')")
public class DivisionController {
	
	@Autowired
	private DivisionService divisionService;

	@Autowired
	private DepartmentAuthorizationService authService;
	
	@PostMapping
	public ResponseEntity<Division> addDivision(@Valid @RequestBody Division division) {
		if (division.getDepartment() != null && division.getDepartment().getId() != null) {
			authService.checkDepartmentAccess(division.getDepartment().getId());
		} else {
			authService.checkSuperAdminAccess();
		}
		Division saved = divisionService.addDivision(division);
		return ResponseEntity.ok(saved);
	}
	
	@GetMapping
	public ResponseEntity<List<Division>> getAllDivisions() {
		List<Division> all = divisionService.getAll();
		if (authService.isInstitutionalAdmin()) {
			return ResponseEntity.ok(all);
		}
		Long deptId = authService.getCurrentUser().getDepartment().getId();
		List<Division> filtered = all.stream()
			.filter(d -> d.getDepartment() != null && deptId.equals(d.getDepartment().getId()))
			.toList();
		return ResponseEntity.ok(filtered);
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<Division> getDivisionById(@PathVariable Long id) {
		authService.checkDivisionAccess(id);
		return ResponseEntity.ok(divisionService.getById(id));
	}

	@GetMapping("/academic-year/{academicYearId}")
	public ResponseEntity<List<Division>> getDivisionsByAcademicYear(@PathVariable Long academicYearId) {
		List<Division> all = divisionService.getByAcademicYear(academicYearId);
		if (authService.isInstitutionalAdmin()) {
			return ResponseEntity.ok(all);
		}
		Long deptId = authService.getCurrentUser().getDepartment().getId();
		List<Division> filtered = all.stream()
			.filter(d -> d.getDepartment() != null && deptId.equals(d.getDepartment().getId()))
			.toList();
		return ResponseEntity.ok(filtered);
	}
	
	@PutMapping("/{id}")
	public ResponseEntity<Division> updateDivision(@PathVariable Long id, @Valid @RequestBody Division division) {
		authService.checkDivisionAccess(id);
		if (division.getDepartment() != null && division.getDepartment().getId() != null) {
			authService.checkDepartmentAccess(division.getDepartment().getId());
		}
		Division updated = divisionService.update(id, division);
		return ResponseEntity.ok(updated);
	}
	
	@DeleteMapping("/{id}")
	public ResponseEntity<String> deleteDivision(@PathVariable Long id) {
		authService.checkDivisionAccess(id);
		divisionService.delete(id);
		return ResponseEntity.ok("Division deleted successfully");
	}
}
