package com.College.timetable.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.College.timetable.Entity.AcademicYear;
import com.College.timetable.Entity.DepartmentEntity;
import com.College.timetable.Entity.Division;
import com.College.timetable.Repository.Acadamic_repo;
import com.College.timetable.Repository.Dep_repo;
import com.College.timetable.Repository.Division_repo;

import jakarta.persistence.EntityNotFoundException;

@Service
public class DivisionService {
	
	@Autowired
	private Division_repo div;
	
	@Autowired
	private Acadamic_repo aca;
	
	@Autowired
	private Dep_repo dep;
	
	public void addDivision(Division division) {
		// Validate academic year exists
		if (division.getAcademicYear() != null && division.getAcademicYear().getId() != null) {
			AcademicYear a = aca.findById(division.getAcademicYear().getId())
				.orElseThrow(() -> new EntityNotFoundException("Academic year not found"));
		}
		
		// Validate department exists
		if (division.getDepartment() != null && division.getDepartment().getId() != null) {
			DepartmentEntity d = dep.findById(division.getDepartment().getId())
				.orElseThrow(() -> new EntityNotFoundException("Department not found"));
		}
		
		div.save(division);
	}
}
