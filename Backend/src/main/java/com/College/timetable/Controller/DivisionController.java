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

import com.College.timetable.Entity.Division;
import com.College.timetable.Service.DivisionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin/api/divisions")
public class DivisionController {
	
	@Autowired
	private DivisionService divisionService;
	
	@PostMapping
	public ResponseEntity<Division> addDivision(@Valid @RequestBody Division division) {
		Division saved = divisionService.addDivision(division);
		return ResponseEntity.ok(saved);
	}
	
	@GetMapping
	public ResponseEntity<List<Division>> getAllDivisions() {
		return ResponseEntity.ok(divisionService.getAll());
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<Division> getDivisionById(@PathVariable Long id) {
		return ResponseEntity.ok(divisionService.getById(id));
	}

	@GetMapping("/academic-year/{academicYearId}")
	public ResponseEntity<List<Division>> getDivisionsByAcademicYear(@PathVariable Long academicYearId) {
		return ResponseEntity.ok(divisionService.getByAcademicYear(academicYearId));
	}
	
	@PutMapping("/{id}")
	public ResponseEntity<Division> updateDivision(@PathVariable Long id, @Valid @RequestBody Division division) {
		Division updated = divisionService.update(id, division);
		return ResponseEntity.ok(updated);
	}
	
	@DeleteMapping("/{id}")
	public ResponseEntity<String> deleteDivision(@PathVariable Long id) {
		divisionService.delete(id);
		return ResponseEntity.ok("Division deleted successfully");
	}
}
