package com.College.timetable.Service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.College.timetable.Entity.CourseEntity;
import com.College.timetable.Entity.DepartmentEntity;
import com.College.timetable.Entity.Semester;
import com.College.timetable.Entity.TimetableStatus;
import com.College.timetable.Repository.Course_repo;
import com.College.timetable.Repository.Dep_repo;
import com.College.timetable.Repository.TimetableEntry_repo;

import jakarta.persistence.EntityNotFoundException;

@Service
public class CourseService {
	
	@Autowired
	private Course_repo course;
	
	@Autowired
	private Dep_repo department;
	
	@Autowired
	private TimetableEntry_repo timetableEntryRepo;
	
	@Autowired
	private com.College.timetable.Repository.Batch_repo batchRepo;
	
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
	
	/**
	 * Get available courses with progressive instances for a division.
	 * Returns courses with their next available instance number based on hours_per_week.
	 * 
	 * Logic:
	 * - For THEORY: hours_per_week = number of instances (e.g., 3 hours = 3 instances) - DIVISION LEVEL
	 * - For LAB: Show course until ALL batches have been allocated - BATCH LEVEL
	 *   - Each batch gets exactly 1 lab session per week (hours_per_week / 2 slots)
	 *   - Hide course only when all batches in division have this lab
	 * - Format: Theory shows instance numbers if > 1, Labs never show instance numbers
	 * - Independent tracking per division (theory) and per batch (lab)
	 */
	@Transactional(readOnly = true)
	public List<java.util.Map<String, Object>> getAvailableCoursesWithCredits(
		Long divisionId,
		Long academicYearId,
		Semester semester
	) {
		// Get all active courses (optionally filtered by semester)
		List<CourseEntity> allCourses = course.findAll().stream()
			.filter(c -> Boolean.TRUE.equals(c.getIsActive()))
			.filter(c -> semester == null || c.getSemester() == semester)
			.toList();
		
		// Get total number of batches in this division from Batch repository
		long totalBatches = batchRepo.findByDivisionId(divisionId).size();
		
		List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
		
		for (CourseEntity courseEntity : allCourses) {
			if (courseEntity.getCourseType() == com.College.timetable.Entity.CourseType.LAB) {
				// LAB LOGIC: Count unique batches that have this lab course
				long batchesWithThisLab = timetableEntryRepo.findAll().stream()
					.filter(e -> e.getDivision() != null && e.getDivision().getId().equals(divisionId))
					.filter(e -> e.getAcademicYear() != null && e.getAcademicYear().getId().equals(academicYearId))
					.filter(e -> e.getCourse() != null && e.getCourse().getId().equals(courseEntity.getId()))
					.filter(e -> e.getStatus() == TimetableStatus.DRAFT)
					.filter(e -> e.getBatch() != null)
					.map(e -> e.getBatch().getId())
					.distinct()
					.count();
				
				// Only show lab if not all batches have been allocated
				// If no batches exist in division, still show the lab course
				if (totalBatches == 0 || batchesWithThisLab < totalBatches) {
					java.util.Map<String, Object> courseWithInstance = new java.util.HashMap<>();
					courseWithInstance.put("id", courseEntity.getId());
					courseWithInstance.put("name", courseEntity.getName());
					courseWithInstance.put("code", courseEntity.getCode());
					courseWithInstance.put("courseType", courseEntity.getCourseType());
					courseWithInstance.put("credits", courseEntity.getCredits());
					courseWithInstance.put("hoursPerWeek", courseEntity.getHoursPerWeek());
					courseWithInstance.put("semester", courseEntity.getSemester());
					courseWithInstance.put("department", courseEntity.getDepartment());
					courseWithInstance.put("displayName", courseEntity.getName()); // No instance number for labs
					courseWithInstance.put("allocatedBatches", batchesWithThisLab);
					courseWithInstance.put("totalBatches", totalBatches);
					result.add(courseWithInstance);
				}
			} else {
				// THEORY LOGIC: Division-level progressive allocation
				int maxInstances = courseEntity.getHoursPerWeek();
				
				// Count existing DRAFT entries for this course in this division
				long existingCount = timetableEntryRepo.findAll().stream()
					.filter(e -> e.getDivision() != null && e.getDivision().getId().equals(divisionId))
					.filter(e -> e.getAcademicYear() != null && e.getAcademicYear().getId().equals(academicYearId))
					.filter(e -> e.getCourse() != null && e.getCourse().getId().equals(courseEntity.getId()))
					.filter(e -> e.getStatus() == TimetableStatus.DRAFT)
					.count();
				
				// Calculate next available instance
				int nextInstance = (int) existingCount + 1;
				
				// Only show if there are still instances available
				if (nextInstance <= maxInstances) {
					java.util.Map<String, Object> courseWithInstance = new java.util.HashMap<>();
					courseWithInstance.put("id", courseEntity.getId());
					courseWithInstance.put("name", courseEntity.getName());
					courseWithInstance.put("code", courseEntity.getCode());
					courseWithInstance.put("courseType", courseEntity.getCourseType());
					courseWithInstance.put("credits", courseEntity.getCredits());
					courseWithInstance.put("hoursPerWeek", courseEntity.getHoursPerWeek());
					courseWithInstance.put("semester", courseEntity.getSemester());
					courseWithInstance.put("department", courseEntity.getDepartment());
					courseWithInstance.put("nextInstance", nextInstance);
					courseWithInstance.put("maxInstances", maxInstances);
					// Only show instance number if max instances > 1
					String displayName = maxInstances > 1 
						? courseEntity.getName() + " (" + nextInstance + ")"
						: courseEntity.getName();
					courseWithInstance.put("displayName", displayName);
					courseWithInstance.put("remainingInstances", maxInstances - existingCount);
					result.add(courseWithInstance);
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Get available batches for a specific lab course.
	 * Returns batches that have NOT been allocated this lab course yet.
	 * Used for dynamic batch filtering when a lab course is selected in the UI.
	 * 
	 * @param courseId The lab course ID
	 * @param divisionId The division ID
	 * @param academicYearId The academic year ID
	 * @return List of batches that don't have this lab course allocated
	 */
	@Transactional(readOnly = true)
	public List<java.util.Map<String, Object>> getAvailableBatchesForCourse(
		Long courseId,
		Long divisionId,
		Long academicYearId
	) {
		// Get all batches for this division
		List<com.College.timetable.Entity.Batch> allBatches = batchRepo.findByDivisionId(divisionId);
		
		// Get batches that already have this lab course allocated
		java.util.Set<Long> allocatedBatchIds = timetableEntryRepo.findAll().stream()
			.filter(e -> e.getDivision() != null && e.getDivision().getId().equals(divisionId))
			.filter(e -> e.getAcademicYear() != null && e.getAcademicYear().getId().equals(academicYearId))
			.filter(e -> e.getCourse() != null && e.getCourse().getId().equals(courseId))
			.filter(e -> e.getStatus() == TimetableStatus.DRAFT)
			.filter(e -> e.getBatch() != null)
			.map(e -> e.getBatch().getId())
			.collect(java.util.stream.Collectors.toSet());
		
		// Filter out allocated batches
		List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
		for (com.College.timetable.Entity.Batch batch : allBatches) {
			if (!allocatedBatchIds.contains(batch.getId())) {
				java.util.Map<String, Object> batchMap = new java.util.HashMap<>();
				batchMap.put("id", batch.getId());
				batchMap.put("name", batch.getName());
				result.add(batchMap);
			}
		}
		
		return result;
	}
}