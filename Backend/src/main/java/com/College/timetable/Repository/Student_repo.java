package com.College.timetable.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.College.timetable.Entity.Student;

@Repository
public interface Student_repo extends JpaRepository<Student, Long> {
	
	// Find students by division
	List<Student> findByDivisionId(Long divisionId);
	
	// Find student by roll number
	Student findByRollNumber(String rollNumber);
}
