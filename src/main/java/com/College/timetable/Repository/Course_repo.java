package com.College.timetable.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.College.timetable.Entity.CourseEntity;

@Repository
public interface Course_repo extends JpaRepository<CourseEntity, Long> {
	
}
