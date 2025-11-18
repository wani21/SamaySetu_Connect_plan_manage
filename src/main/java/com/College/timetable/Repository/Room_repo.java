package com.College.timetable.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.College.timetable.Entity.ClassRoom;

@Repository
public interface Room_repo extends JpaRepository<ClassRoom, Long> {
	
}
