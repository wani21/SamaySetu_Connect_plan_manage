package com.College.timetable.Service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.College.timetable.Entity.AcademicYear;
import com.College.timetable.Repository.Acadamic_repo;

import jakarta.persistence.EntityNotFoundException;

@Service
public class AcadamicService {
	
	@Autowired
	private Acadamic_repo acadamy;

	public AcademicYear addAcadamic(AcademicYear aca) {
		// If this academic year is being set as current, unset any existing current year
		if (aca.getIsCurrent() != null && aca.getIsCurrent()) {
			AcademicYear existingCurrent = acadamy.findByIsCurrent(true);
			if (existingCurrent != null) {
				throw new IllegalArgumentException("An academic year '" + existingCurrent.getYearName() + "' is already set as current. Please unset it first before setting a new current year.");
			}
		}
		return acadamy.save(aca);
	}
	
	public List<AcademicYear> getAll() {
		return acadamy.findAll();
	}
	
	public AcademicYear getById(Long id) {
		return acadamy.findById(id)
			.orElseThrow(() -> new EntityNotFoundException("Academic year not found with id: " + id));
	}
	
	public AcademicYear update(Long id, AcademicYear aca) {
		AcademicYear existing = getById(id);
		
		// If trying to set this as current, check if another year is already current
		if (aca.getIsCurrent() != null && aca.getIsCurrent() && !existing.getIsCurrent()) {
			AcademicYear existingCurrent = acadamy.findByIsCurrent(true);
			if (existingCurrent != null && !existingCurrent.getId().equals(id)) {
				throw new IllegalArgumentException("Academic year '" + existingCurrent.getYearName() + "' is already set as current. Please unset it first before setting '" + aca.getYearName() + "' as current.");
			}
		}
		
		existing.setYearName(aca.getYearName());
		existing.setStartDate(aca.getStartDate());
		existing.setEndDate(aca.getEndDate());
		existing.setIsCurrent(aca.getIsCurrent());
		return acadamy.save(existing);
	}
	
	public void delete(Long id) {
		if (!acadamy.existsById(id)) {
			throw new EntityNotFoundException("Academic year not found with id: " + id);
		}
		acadamy.deleteById(id);
	}
}
