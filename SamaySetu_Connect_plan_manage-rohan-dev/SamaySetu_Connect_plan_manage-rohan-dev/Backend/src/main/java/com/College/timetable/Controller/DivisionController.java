package com.College.timetable.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.College.timetable.Entity.Division;
import com.College.timetable.Service.DivisionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("admin/api/divisions")
public class DivisionController {
	
	@Autowired
	private DivisionService divisionService;
	
	@PostMapping
	public ResponseEntity<String> addDivision(@Valid @RequestBody Division division) {
		divisionService.addDivision(division);
		return ResponseEntity.ok("Division added successfully");
	}
}
