package com.College.timetable.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.College.timetable.Entity.TimeSlot;

@Repository
public interface TimeSlot_repo extends JpaRepository<TimeSlot, Long> {
	
}
