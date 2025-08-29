package com.example.Review1.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.Review1.Entity.TeacherEntity;

public interface Teacher_Repo extends JpaRepository<TeacherEntity, Long>{

}
