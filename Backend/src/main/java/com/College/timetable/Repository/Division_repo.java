package com.College.timetable.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.College.timetable.Entity.Division;

@Repository
public interface Division_repo extends JpaRepository<Division, Long> {
	
}
