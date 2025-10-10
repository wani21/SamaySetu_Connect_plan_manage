package com.College.timetable.Controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.College.timetable.Entity.DepartmentEntity;
import com.College.timetable.Service.DepartmentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {
	
	private static final Logger log = LoggerFactory.getLogger(DepartmentController.class);
	
	@Autowired
	private DepartmentService departmentService;
	
	@PostMapping
	public ResponseEntity<String> addDepartment(@Valid @RequestBody DepartmentEntity dep) {
		log.info("Adding new department: {}", dep.getName());
		departmentService.addDep(dep);
		return ResponseEntity.ok("Department added successfully");
	}
	
	@GetMapping
	public ResponseEntity<List<DepartmentEntity>> getAllDepartments() {
		log.info("Fetching all departments");
		return ResponseEntity.ok(departmentService.getall());
	}
}
