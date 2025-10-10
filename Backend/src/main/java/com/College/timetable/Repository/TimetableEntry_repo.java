package com.College.timetable.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.College.timetable.Entity.DayOfWeek;
import com.College.timetable.Entity.TimetableEntry;

@Repository
public interface TimetableEntry_repo extends JpaRepository<TimetableEntry, Long> {
	
	// Find by division
	List<TimetableEntry> findByDivisionIdAndAcademicYearId(Long divisionId, Long academicYearId);
	
	// Find by teacher
	List<TimetableEntry> findByTeacherIdAndAcademicYearId(Long teacherId, Long academicYearId);
	
	// Check teacher conflict
	@Query("SELECT COUNT(te) FROM TimetableEntry te WHERE " +
	       "te.teacher.id = :teacherId AND " +
	       "te.dayOfWeek = :dayOfWeek AND " +
	       "te.timeSlot.id = :timeSlotId AND " +
	       "te.academicYear.id = :academicYearId AND " +
	       "(:entryId IS NULL OR te.id != :entryId)")
	long countTeacherConflicts(
		@Param("teacherId") Long teacherId,
		@Param("dayOfWeek") DayOfWeek dayOfWeek,
		@Param("timeSlotId") Long timeSlotId,
		@Param("academicYearId") Long academicYearId,
		@Param("entryId") Long entryId
	);
	
	// Check room conflict
	@Query("SELECT COUNT(te) FROM TimetableEntry te WHERE " +
	       "te.room.id = :roomId AND " +
	       "te.dayOfWeek = :dayOfWeek AND " +
	       "te.timeSlot.id = :timeSlotId AND " +
	       "te.academicYear.id = :academicYearId AND " +
	       "(:entryId IS NULL OR te.id != :entryId)")
	long countRoomConflicts(
		@Param("roomId") Long roomId,
		@Param("dayOfWeek") DayOfWeek dayOfWeek,
		@Param("timeSlotId") Long timeSlotId,
		@Param("academicYearId") Long academicYearId,
		@Param("entryId") Long entryId
	);
	
	// Calculate teacher weekly hours
	@Query("SELECT COALESCE(SUM(ts.durationMinutes), 0) FROM TimetableEntry te " +
	       "JOIN te.timeSlot ts WHERE " +
	       "te.teacher.id = :teacherId AND " +
	       "te.academicYear.id = :academicYearId AND " +
	       "(:entryId IS NULL OR te.id != :entryId)")
	Integer calculateTeacherWeeklyMinutes(
		@Param("teacherId") Long teacherId,
		@Param("academicYearId") Long academicYearId,
		@Param("entryId") Long entryId
	);
}
