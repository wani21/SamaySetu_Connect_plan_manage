package com.College.timetable.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.College.timetable.Entity.DepartmentEntity;

@Repository
public interface Dep_repo extends JpaRepository<DepartmentEntity, Long> {
	
}
