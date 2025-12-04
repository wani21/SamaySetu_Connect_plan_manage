package com.College.timetable.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.College.timetable.Entity.AcademicYear;

@Repository
public interface Acadamic_repo extends JpaRepository<AcademicYear, Long> {
	
	// Find the current academic year
	AcademicYear findByIsCurrent(Boolean isCurrent);
	
	// Count how many academic years are marked as current
	long countByIsCurrent(Boolean isCurrent);
}
