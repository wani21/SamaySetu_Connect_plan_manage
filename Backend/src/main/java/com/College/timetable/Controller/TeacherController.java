package com.College.timetable.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.College.timetable.Entity.TeacherEntity;
import com.College.timetable.Service.TeacherService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/teachers")
public class TeacherController {

	@Autowired
	private TeacherService teacherService;
	
	@PostMapping
	public ResponseEntity<String> addTeacher(@Valid @RequestBody TeacherEntity teach) {
		teacherService.add(teach);
		return ResponseEntity.ok("Teacher added successfully");
	}
}
