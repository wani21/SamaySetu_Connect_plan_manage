package com.College.timetable.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.College.timetable.Entity.AcademicYear;
import com.College.timetable.Service.AcadamicService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("admin/api/academic-years")
public class AcadamicController {
	
	@Autowired
	private AcadamicService academicService;
	
	@PostMapping
	public ResponseEntity<String> addAcademicYear(@Valid @RequestBody AcademicYear aca) {
		academicService.addAcadamic(aca);
		return ResponseEntity.ok("Academic year added successfully");
	}
}
