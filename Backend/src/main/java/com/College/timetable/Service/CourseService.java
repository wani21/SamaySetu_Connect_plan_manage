package com.College.timetable.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.College.timetable.Entity.CourseEntity;
import com.College.timetable.Entity.DepartmentEntity;
import com.College.timetable.Repository.Course_repo;
import com.College.timetable.Repository.Dep_repo;

import jakarta.persistence.EntityNotFoundException;

@Service
public class CourseService {
	
	@Autowired
	private Course_repo course;
	
	@Autowired
	private Dep_repo department;
	
	public void add(CourseEntity c) {
		// Validate department exists
		if (c.getDepartment() != null && c.getDepartment().getId() != null) {
			DepartmentEntity depart = department.findById(c.getDepartment().getId())
				.orElseThrow(() -> new EntityNotFoundException("Department not found"));
		}
		course.save(c);
	}
}
