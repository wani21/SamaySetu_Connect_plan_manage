package com.College.timetable.Service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.College.timetable.Entity.AcademicYear;
import com.College.timetable.Entity.DepartmentEntity;
import com.College.timetable.Repository.AcademicYearRepository;
import com.College.timetable.Repository.Dep_repo;

import jakarta.persistence.EntityNotFoundException;

@Service
public class DepartmentService {

	@Autowired
	private Dep_repo dep;
	
	@Autowired
	private AcademicYearRepository academicYearRepo;

	@Transactional
	@org.springframework.cache.annotation.CacheEvict(value = "departments", allEntries = true)
	public DepartmentEntity addDep(DepartmentEntity d) {
		return dep.save(d);
	}

	@Transactional(readOnly = true)
	@org.springframework.cache.annotation.Cacheable("departments")
	public List<DepartmentEntity> getall() {
		return dep.findAll();
	}

	@Transactional(readOnly = true)
	public List<DepartmentEntity> getByAcademicYear(Long academicYearId) {
		return dep.findByAcademicYearId(academicYearId);
	}

	@Transactional(readOnly = true)
	@org.springframework.cache.annotation.Cacheable(value = "departments", key = "#id")
	public DepartmentEntity getById(Long id) {
		return dep.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Department not found with id: " + id));
	}

	@Transactional
	@org.springframework.cache.annotation.CacheEvict(value = "departments", allEntries = true)
	public DepartmentEntity update(Long id, DepartmentEntity d) {
		DepartmentEntity existing = getById(id);
		existing.setName(d.getName());
		existing.setCode(d.getCode());
		existing.setHeadOfDepartment(d.getHeadOfDepartment());
		existing.setYears(d.getYears());
		return dep.save(existing);
	}

	@Transactional
	@org.springframework.cache.annotation.CacheEvict(value = "departments", allEntries = true)
	public void delete(Long id) {
		if (!dep.existsById(id)) {
			throw new EntityNotFoundException("Department not found with id: " + id);
		}
		dep.deleteById(id);
	}

	@Transactional
	@org.springframework.cache.annotation.CacheEvict(value = "departments", allEntries = true)
	public List<DepartmentEntity> copyDepartmentsToAcademicYear(Long sourceAcademicYearId, Long targetAcademicYearId, List<Long> departmentIds) {
		AcademicYear targetYear = academicYearRepo.findById(targetAcademicYearId)
				.orElseThrow(() -> new EntityNotFoundException("Target academic year not found"));
		
		List<DepartmentEntity> copiedDepartments = new ArrayList<>();
		
		for (Long deptId : departmentIds) {
			DepartmentEntity source = getById(deptId);
			
			DepartmentEntity newDept = new DepartmentEntity();
			newDept.setName(source.getName());
			newDept.setCode(source.getCode());
			newDept.setHeadOfDepartment(source.getHeadOfDepartment());
			newDept.setYears(source.getYears());
			newDept.setAcademicYear(targetYear);
			
			copiedDepartments.add(dep.save(newDept));
		}
		
		return copiedDepartments;
	}
}
