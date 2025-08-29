package com.example.Review1.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.Review1.Entity.CourseEntity;

public interface Course_repo extends JpaRepository<CourseEntity, Long>{

}
