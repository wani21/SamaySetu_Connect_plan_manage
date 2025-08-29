package com.example.Review1.Conttoller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.Review1.Entity.TeacherEntity;
import com.example.Review1.Service.TeacherService;

@RestController
@RequestMapping("/Teacher")
public class TeacherController {

	@Autowired
	TeacherService teacher;
	
	
	@PostMapping("/add")
	public void add(@RequestBody TeacherEntity teach) {
	    teacher.add(teach);
	}

	
	
}
