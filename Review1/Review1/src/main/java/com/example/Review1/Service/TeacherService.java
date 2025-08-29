package com.example.Review1.Service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.stereotype.Service;

import com.example.Review1.Entity.CourseEntity;
import com.example.Review1.Entity.DepartmentEntity;
import com.example.Review1.Entity.TeacherEntity;
import com.example.Review1.Repository.Course_repo;
import com.example.Review1.Repository.Dep_repo;
import com.example.Review1.Repository.Teacher_Repo;

import jakarta.persistence.EntityNotFoundException;

@Service
public class TeacherService {
	
	
	@Autowired
	Teacher_Repo teacher;
	
	@Autowired
	Dep_repo department;
	
	@Autowired
	Course_repo course;
	
	public void add(TeacherEntity teach) {
	    DepartmentEntity depart = department.findById(teach.getDepartment())
	        .orElseThrow(() -> new EntityNotFoundException("The department id is not found"));

	    CourseEntity cor = course.findById(teach.getCourse())
	        .orElseThrow(() -> new EntityNotFoundException("The course id is not found"));

	    teacher.save(teach);
	}
	
}
