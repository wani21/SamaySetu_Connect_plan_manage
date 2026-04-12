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

import com.College.timetable.Entity.AcademicYear;
import com.College.timetable.Service.AcademicService;

import jakarta.validation.Valid;

@RestController
public class AcademicController {

	@Autowired
	private AcademicService academicService;

	// ── Public endpoint (teachers + admins) ──
	// Accessible to any authenticated user — needed by teacher dashboard, timetable, availability pages
	@GetMapping("/api/academic-years")
	public ResponseEntity<List<AcademicYear>> getAcademicYearsPublic() {
		return ResponseEntity.ok(academicService.getAll());
	}

	@GetMapping("/api/academic-years/current")
	public ResponseEntity<AcademicYear> getCurrentAcademicYearPublic() {
		return ResponseEntity.ok(
			academicService.getAll().stream()
				.filter(y -> Boolean.TRUE.equals(y.getIsCurrent()))
				.findFirst()
				.orElse(null)
		);
	}

	// ── Admin-only endpoints ──
	@PostMapping("/admin/api/academic-years")
	public ResponseEntity<AcademicYear> addAcademicYear(@Valid @RequestBody AcademicYear aca) {
		AcademicYear saved = academicService.addAcademic(aca);
		return ResponseEntity.ok(saved);
	}

	@GetMapping("/admin/api/academic-years")
	public ResponseEntity<List<AcademicYear>> getAllAcademicYears() {
		return ResponseEntity.ok(academicService.getAll());
	}

	@GetMapping("/admin/api/academic-years/{id}")
	public ResponseEntity<AcademicYear> getAcademicYearById(@PathVariable Long id) {
		return ResponseEntity.ok(academicService.getById(id));
	}

	@PutMapping("/admin/api/academic-years/{id}")
	public ResponseEntity<AcademicYear> updateAcademicYear(@PathVariable Long id, @Valid @RequestBody AcademicYear aca) {
		AcademicYear updated = academicService.update(id, aca);
		return ResponseEntity.ok(updated);
	}

	@DeleteMapping("/admin/api/academic-years/{id}")
	public ResponseEntity<String> deleteAcademicYear(@PathVariable Long id) {
		academicService.delete(id);
		return ResponseEntity.ok("Academic year deleted successfully");
	}
}
