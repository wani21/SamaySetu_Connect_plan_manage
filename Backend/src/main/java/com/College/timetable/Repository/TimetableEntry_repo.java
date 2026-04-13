package com.College.timetable.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.College.timetable.Entity.AcademicYear;

import com.College.timetable.Entity.ClassRoom;
import com.College.timetable.Entity.DayOfWeek;
import com.College.timetable.Entity.Division;
import com.College.timetable.Entity.TeacherEntity;
import com.College.timetable.Entity.TimeSlot;
import com.College.timetable.Entity.TimetableEntry;
import com.College.timetable.Entity.TimetableStatus;

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
	
	

    // Teacher conflict check (Academic year aware)
    boolean existsByDayOfWeekAndTimeSlotAndTeacherAndAcademicYear(
            DayOfWeek dayOfWeek,
            TimeSlot timeSlot,
            TeacherEntity teacher,
            AcademicYear academicYear
    );

    // Room conflict check
    boolean existsByDayOfWeekAndTimeSlotAndRoomAndAcademicYear(
            DayOfWeek dayOfWeek,
            TimeSlot timeSlot,
            ClassRoom room,
            AcademicYear academicYear
    );

    // Division conflict check
    boolean existsByDayOfWeekAndTimeSlotAndDivisionAndAcademicYear(
            DayOfWeek dayOfWeek,
            TimeSlot timeSlot,
            Division division,
            AcademicYear academicYear
    );
    
    @Query("""
            SELECT COUNT(t) > 0 FROM TimetableEntry t
            WHERE t.teacher.id = :teacherId
            AND t.dayOfWeek = :day
            AND t.timeSlot.id = :timeSlotId
            AND t.academicYear.id = :academicYearId
            AND t.labSessionGroup IS NULL
            AND (:excludeId IS NULL OR t.id != :excludeId)
            """)
        boolean isTeacherBooked(
            @Param("teacherId") Long teacherId,
            @Param("day") DayOfWeek day,
            @Param("timeSlotId") Long timeSlotId,
            @Param("academicYearId") Long academicYearId,
            @Param("excludeId") Long excludeId
        );

        /**
         * Check if room is already booked at this day + slot + year
         * Always enforced — even for lab sessions (each batch uses a DIFFERENT room)
         */
        @Query("""
            SELECT COUNT(t) > 0 FROM TimetableEntry t
            WHERE t.room.id = :roomId
            AND t.dayOfWeek = :day
            AND t.timeSlot.id = :timeSlotId
            AND t.academicYear.id = :academicYearId
            AND (:excludeId IS NULL OR t.id != :excludeId)
            """)
        boolean isRoomBooked(
            @Param("roomId") Long roomId,
            @Param("day") DayOfWeek day,
            @Param("timeSlotId") Long timeSlotId,
            @Param("academicYearId") Long academicYearId,
            @Param("excludeId") Long excludeId
        );

        /**
         * Check if division already has a class at this day + slot + year
         * Excludes lab session entries (division splits into batches — intentional parallel)
         */
        @Query("""
            SELECT COUNT(t) > 0 FROM TimetableEntry t
            WHERE t.division.id = :divisionId
            AND t.dayOfWeek = :day
            AND t.timeSlot.id = :timeSlotId
            AND t.academicYear.id = :academicYearId
            AND t.labSessionGroup IS NULL
            AND (:excludeId IS NULL OR t.id != :excludeId)
            """)
        boolean isDivisionBooked(
            @Param("divisionId") Long divisionId,
            @Param("day") DayOfWeek day,
            @Param("timeSlotId") Long timeSlotId,
            @Param("academicYearId") Long academicYearId,
            @Param("excludeId") Long excludeId
        );

        /**
         * Count how many hours a teacher is assigned in a given academic year
         * Used to enforce max_weekly_hours constraint
         */
        @Query("""
            SELECT COUNT(t) FROM TimetableEntry t
            WHERE t.teacher.id = :teacherId
            AND t.academicYear.id = :academicYearId
            AND t.dayOfWeek = :day
            AND t.status != 'ARCHIVED'
            """)
        long countTeacherPeriodsOnDay(
            @Param("teacherId") Long teacherId,
            @Param("academicYearId") Long academicYearId,
            @Param("day") DayOfWeek day
        );

        /**
         * Count total weekly hours for a teacher in an academic year
         */
        @Query("""
            SELECT COUNT(t) FROM TimetableEntry t
            WHERE t.teacher.id = :teacherId
            AND t.academicYear.id = :academicYearId
            AND t.status != 'ARCHIVED'
            """)
        long countTeacherWeeklyHours(
            @Param("teacherId") Long teacherId,
            @Param("academicYearId") Long academicYearId
        );

        // ---------------------------------------------------------------
        // TIMETABLE READ QUERIES (served from Redis cache in service layer)
        // ---------------------------------------------------------------

        /**
         * Get full published timetable for a division
         * This is the most-read query in the system — cached in Redis
         */
        List<TimetableEntry> findByDivisionIdAndAcademicYearIdAndStatus(
            Long divisionId,
            Long academicYearId,
            TimetableStatus status
        );

        /**
         * Get published timetable for a teacher
         */
        List<TimetableEntry> findByTeacherIdAndAcademicYearIdAndStatus(
            Long teacherId,
            Long academicYearId,
            TimetableStatus status
        );

        /**
         * Get all entries for a specific day + division (for daily view)
         */
        List<TimetableEntry> findByDivisionIdAndAcademicYearIdAndDayOfWeekAndStatus(
            Long divisionId,
            Long academicYearId,
            DayOfWeek day,
            TimetableStatus status
        );

        /**
         * Get all DRAFT entries for a division (admin review before publishing)
         */
        List<TimetableEntry> findByDivisionIdAndAcademicYearIdAndStatusOrderByDayOfWeekAscTimeSlotAsc(
            Long divisionId,
            Long academicYearId,
            TimetableStatus status
        );

        /**
         * Get all entries in a lab session group
         */
        List<TimetableEntry> findByLabSessionGroupId(Long labSessionGroupId);

        /**
         * Publish all DRAFT entries for a division — flip to PUBLISHED
         */
        @Modifying
        @Query("""
            UPDATE TimetableEntry t SET t.status = 'PUBLISHED'
            WHERE t.division.id = :divisionId
            AND t.academicYear.id = :academicYearId
            AND t.status = 'DRAFT'
            """)
        int publishDivisionTimetable(
            @Param("divisionId") Long divisionId,
            @Param("academicYearId") Long academicYearId
        );

        /**
         * Archive all PUBLISHED entries for a division — end of semester
         */
        @Modifying
        @Query("""
            UPDATE TimetableEntry t SET t.status = 'ARCHIVED'
            WHERE t.division.id = :divisionId
            AND t.academicYear.id = :academicYearId
            AND t.status = 'PUBLISHED'
            """)
        int archiveDivisionTimetable(
            @Param("divisionId") Long divisionId,
            @Param("academicYearId") Long academicYearId
        );

        /**
         * Delete all DRAFT entries for a division (reset and start fresh)
         */
        @Modifying
        @Query("""
            DELETE FROM TimetableEntry t
            WHERE t.division.id = :divisionId
            AND t.academicYear.id = :academicYearId
            AND t.status = 'DRAFT'
            """)
        int clearDraftTimetable(
            @Param("divisionId") Long divisionId,
            @Param("academicYearId") Long academicYearId
        );
}
