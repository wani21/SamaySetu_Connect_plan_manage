package com.College.timetable.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.College.timetable.Entity.TeacherEntity;

@Repository
public interface Teacher_Repo extends JpaRepository<TeacherEntity, Long> {
	
	Optional<TeacherEntity> findbyEmail(String email);
	
}
