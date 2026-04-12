package com.College.timetable.Repository;

import java.time.LocalTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.College.timetable.Entity.TimeSlot;

@Repository
public interface TimeSlot_repo extends JpaRepository<TimeSlot, Long> {
    List<TimeSlot> findByType(String type);
    List<TimeSlot> findByIsActiveTrue();
    List<TimeSlot> findByTypeAndIsActiveTrue(String type);
    
    // Check for overlapping time slots within the same type
    @Query("SELECT t FROM TimeSlot t WHERE t.type = :type AND t.id != :excludeId AND " +
           "((t.startTime < :endTime AND t.endTime > :startTime))")
    List<TimeSlot> findOverlappingSlots(@Param("type") String type, 
                                        @Param("startTime") LocalTime startTime, 
                                        @Param("endTime") LocalTime endTime,
                                        @Param("excludeId") Long excludeId);
    
    @Query("SELECT t FROM TimeSlot t WHERE t.type = :type AND " +
           "((t.startTime < :endTime AND t.endTime > :startTime))")
    List<TimeSlot> findOverlappingSlotsForNew(@Param("type") String type,
                                              @Param("startTime") LocalTime startTime,
                                              @Param("endTime") LocalTime endTime);

    /**
     * Find the immediate next slot (break or lecture) after a given time.
     * Used to check whether a lab can span 2 truly consecutive periods
     * with no break in between.
     */
    @Query("SELECT t FROM TimeSlot t WHERE t.type = :type AND t.isActive = true " +
           "AND t.startTime >= :afterTime ORDER BY t.startTime ASC LIMIT 1")
    java.util.Optional<TimeSlot> findImmediateNextSlot(
        @Param("type") String type,
        @Param("afterTime") LocalTime afterTime
    );
}
