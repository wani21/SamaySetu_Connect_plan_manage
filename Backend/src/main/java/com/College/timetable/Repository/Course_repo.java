package com.College.timetable.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.College.timetable.Entity.CourseEntity;

@Repository
public interface Course_repo extends JpaRepository<CourseEntity, Long> {
	long countByDepartmentId(Long departmentId);
	
	// Check if short name exists for a specific department and year
	@Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM CourseEntity c " +
	       "WHERE UPPER(c.shortName) = UPPER(:shortName) " +
	       "AND c.department.id = :departmentId " +
	       "AND c.year = :year")
	boolean existsByShortNameAndDepartmentAndYear(
		@Param("shortName") String shortName,
		@Param("departmentId") Long departmentId,
		@Param("year") Integer year
	);
	
	// Find course by short name, department, and year
	@Query("SELECT c FROM CourseEntity c " +
	       "WHERE UPPER(c.shortName) = UPPER(:shortName) " +
	       "AND c.department.id = :departmentId " +
	       "AND c.year = :year")
	Optional<CourseEntity> findByShortNameAndDepartmentAndYear(
		@Param("shortName") String shortName,
		@Param("departmentId") Long departmentId,
		@Param("year") Integer year
	);
}
