package com.College.timetable.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.College.timetable.Entity.CourseEntity;
import com.College.timetable.Service.CourseService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("admin/api/courses")
public class CourseController {
	
	@Autowired
	private CourseService courseService;
	
	@PostMapping
	public ResponseEntity<String> addCourse(@Valid @RequestBody CourseEntity cor) {
		courseService.add(cor);
		return ResponseEntity.ok("Course added successfully");
	}
}
