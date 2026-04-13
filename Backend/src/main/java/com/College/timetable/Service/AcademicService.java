package com.College.timetable.Service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.College.timetable.Entity.AcademicYear;
import com.College.timetable.Repository.AcademicYearRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class AcademicService {

	@Autowired
	private AcademicYearRepository academicYearRepo;

	@Transactional
	@CacheEvict(value = "academic-years", allEntries = true)
	public AcademicYear addAcademic(AcademicYear aca) {
		// If this academic year is being set as current, unset any existing current year
		if (aca.getIsCurrent() != null && aca.getIsCurrent()) {
			AcademicYear existingCurrent = academicYearRepo.findByIsCurrent(true);
			if (existingCurrent != null) {
				throw new IllegalArgumentException("An academic year '" + existingCurrent.getYearName() + "' is already set as current. Please unset it first before setting a new current year.");
			}
		}
		return academicYearRepo.save(aca);
	}

	@Transactional(readOnly = true)
	@Cacheable("academic-years")
	public List<AcademicYear> getAll() {
		return academicYearRepo.findAll();
	}

	@Transactional(readOnly = true)
	@Cacheable(value = "academic-years", key = "#id")
	public AcademicYear getById(Long id) {
		return academicYearRepo.findById(id)
			.orElseThrow(() -> new EntityNotFoundException("Academic year not found with id: " + id));
	}

	@Transactional
	@CacheEvict(value = "academic-years", allEntries = true)
	public AcademicYear update(Long id, AcademicYear aca) {
		AcademicYear existing = getById(id);

		// If trying to set this as current, check if another year is already current
		if (aca.getIsCurrent() != null && aca.getIsCurrent() && !existing.getIsCurrent()) {
			AcademicYear existingCurrent = academicYearRepo.findByIsCurrent(true);
			if (existingCurrent != null && !existingCurrent.getId().equals(id)) {
				throw new IllegalArgumentException("Academic year '" + existingCurrent.getYearName() + "' is already set as current. Please unset it first before setting '" + aca.getYearName() + "' as current.");
			}
		}

		existing.setYearName(aca.getYearName());
		existing.setStartDate(aca.getStartDate());
		existing.setEndDate(aca.getEndDate());
		existing.setIsCurrent(aca.getIsCurrent());
		return academicYearRepo.save(existing);
	}

	@Transactional
	@CacheEvict(value = "academic-years", allEntries = true)
	public void delete(Long id) {
		if (!academicYearRepo.existsById(id)) {
			throw new EntityNotFoundException("Academic year not found with id: " + id);
		}
		academicYearRepo.deleteById(id);
	}
}
