package com.College.timetable.Repository;

import java.time.LocalTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.College.timetable.Entity.DayOfWeek;
import com.College.timetable.Entity.TeacherAvailability;

@Repository
public interface TeacherAvailability_repo extends JpaRepository<TeacherAvailability, Long> {
	
	// Find availability by teacher and day
	List<TeacherAvailability> findByTeacherIdAndDayOfWeek(Long teacherId, DayOfWeek dayOfWeek);
	
	// Check if teacher is available during a time slot
	@Query("SELECT COUNT(ta) FROM TeacherAvailability ta WHERE " +
	       "ta.teacher.id = :teacherId AND " +
	       "ta.dayOfWeek = :dayOfWeek AND " +
	       "ta.startTime <= :slotStart AND " +
	       "ta.endTime >= :slotEnd AND " +
	       "ta.isAvailable = true")
	long countAvailability(
		@Param("teacherId") Long teacherId,
		@Param("dayOfWeek") DayOfWeek dayOfWeek,
		@Param("slotStart") LocalTime slotStart,
		@Param("slotEnd") LocalTime slotEnd
	);
}
