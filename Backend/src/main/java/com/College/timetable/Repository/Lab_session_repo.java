package com.College.timetable.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.College.timetable.Entity.LabSessionGroup;

public interface Lab_session_repo extends JpaRepository<LabSessionGroup, Long>{

}
