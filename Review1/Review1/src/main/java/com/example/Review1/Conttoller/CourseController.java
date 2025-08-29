package com.example.Review1.Conttoller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.Review1.Entity.CourseEntity;
import com.example.Review1.Service.CourseService;

@RestController
@RequestMapping("/Course")
public class CourseController {

	
	@Autowired
	CourseService course;
	
	@PostMapping("/add")
	public void addCourse(@RequestBody CourseEntity cor) {
		course.add(cor);
	}
}
