package com.College.timetable.Service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.College.timetable.Entity.AcademicYear;
import com.College.timetable.Entity.DepartmentEntity;
import com.College.timetable.Entity.Division;
import com.College.timetable.Repository.AcademicYearRepository;
import com.College.timetable.Repository.Dep_repo;
import com.College.timetable.Repository.Division_repo;

import jakarta.persistence.EntityNotFoundException;

@Service
public class DivisionService {
	
	@Autowired
	private Division_repo div;
	
	@Autowired
	private AcademicYearRepository aca;
	
	@Autowired
	private Dep_repo dep;
	
	@Transactional
	public Division addDivision(Division division) {
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

		return div.save(division);
	}

	@Transactional(readOnly = true)
	public List<Division> getAll() {
		return div.findAll();
	}

	@Transactional(readOnly = true)
	public List<Division> getByAcademicYear(Long academicYearId) {
		return div.findByAcademicYearId(academicYearId);
	}

	@Transactional(readOnly = true)
	public Division getById(Long id) {
		return div.findById(id)
			.orElseThrow(() -> new EntityNotFoundException("Division not found with id: " + id));
	}

	@Transactional
	public Division update(Long id, Division division) {
		Division existing = getById(id);
		existing.setName(division.getName());
		existing.setYear(division.getYear());
		existing.setBranch(division.getBranch());
		existing.setTotalStudents(division.getTotalStudents());
		existing.setIsActive(division.getIsActive());
		existing.setTimeSlotType(division.getTimeSlotType());
		existing.setClassTeacher(division.getClassTeacher());
		existing.setClassRepresentative(division.getClassRepresentative());

		// Update academic year if provided
		if (division.getAcademicYear() != null && division.getAcademicYear().getId() != null) {
			AcademicYear a = aca.findById(division.getAcademicYear().getId())
				.orElseThrow(() -> new EntityNotFoundException("Academic year not found"));
			existing.setAcademicYear(a);
		}

		// Update department if provided
		if (division.getDepartment() != null && division.getDepartment().getId() != null) {
			DepartmentEntity d = dep.findById(division.getDepartment().getId())
				.orElseThrow(() -> new EntityNotFoundException("Department not found"));
			existing.setDepartment(d);
		}

		return div.save(existing);
	}

	@Transactional
	public void delete(Long id) {
		if (!div.existsById(id)) {
			throw new EntityNotFoundException("Division not found with id: " + id);
		}
		div.deleteById(id);
	}
}
