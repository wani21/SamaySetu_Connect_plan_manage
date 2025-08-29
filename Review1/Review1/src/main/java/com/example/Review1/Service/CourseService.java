package com.example.Review1.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.Review1.Entity.CourseEntity;
import com.example.Review1.Repository.Course_repo;

@Service
public class CourseService {

	
	@Autowired
	Course_repo course;
	
	public void add(CourseEntity c) {
		course.save(c);
	}
}
