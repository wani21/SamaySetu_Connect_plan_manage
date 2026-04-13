package com.College.timetable.Service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
	
	@Transactional
	public CourseEntity add(CourseEntity c) {
		// Validate department exists
		if (c.getDepartment() != null && c.getDepartment().getId() != null) {
			DepartmentEntity depart = department.findById(c.getDepartment().getId())
				.orElseThrow(() -> new EntityNotFoundException("Department not found"));
		}
		return course.save(c);
	}

	@Transactional(readOnly = true)
	public List<CourseEntity> getAll() {
		return course.findAll();
	}

	@Transactional(readOnly = true)
	public CourseEntity getById(Long id) {
		return course.findById(id)
			.orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + id));
	}

	@Transactional
	public CourseEntity update(Long id, CourseEntity c) {
		CourseEntity existing = getById(id);
		existing.setName(c.getName());
		existing.setCode(c.getCode());
		existing.setCourseType(c.getCourseType());
		existing.setCredits(c.getCredits());
		existing.setHoursPerWeek(c.getHoursPerWeek());
		existing.setSemester(c.getSemester());
		existing.setDescription(c.getDescription());
		existing.setPrerequisites(c.getPrerequisites());
		existing.setIsActive(c.getIsActive());
		existing.setYear(c.getYear());

		// Update department if provided
		if (c.getDepartment() != null && c.getDepartment().getId() != null) {
			DepartmentEntity depart = department.findById(c.getDepartment().getId())
				.orElseThrow(() -> new EntityNotFoundException("Department not found"));
			existing.setDepartment(depart);
		}

		return course.save(existing);
	}

	@Transactional
	public void delete(Long id) {
		if (!course.existsById(id)) {
			throw new EntityNotFoundException("Course not found with id: " + id);
		}
		course.deleteById(id);
	}
}
