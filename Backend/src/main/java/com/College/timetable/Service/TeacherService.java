package com.College.timetable.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.College.timetable.Entity.DepartmentEntity;
import com.College.timetable.Entity.TeacherEntity;
import com.College.timetable.Repository.Dep_repo;
import com.College.timetable.Repository.Teacher_Repo;

import jakarta.persistence.EntityNotFoundException;

@Service
public class TeacherService {
	
	@Autowired
	private Teacher_Repo teacher;
	
	@Autowired
	private Dep_repo department;
	
	public void add(TeacherEntity teach) {
		// Validate department exists
		if (teach.getDepartment() != null && teach.getDepartment().getId() != null) {
			DepartmentEntity depart = department.findById(teach.getDepartment().getId())
				.orElseThrow(() -> new EntityNotFoundException("Department not found"));
		}
		teacher.save(teach);
	}
}
