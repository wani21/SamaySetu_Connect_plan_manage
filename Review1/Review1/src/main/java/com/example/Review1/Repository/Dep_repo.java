package com.example.Review1.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.Review1.Entity.DepartmentEntity;

public interface Dep_repo extends JpaRepository<DepartmentEntity, Long>{

}
