package com.example.Review1.Service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.Review1.Entity.DepartmentEntity;
import com.example.Review1.Repository.Dep_repo;

@Service
public class DepartmentService {
	
	@Autowired
	Dep_repo dep;
	
	public void addDep(DepartmentEntity d) {
		dep.save(d);
	}
	
	
	public List<DepartmentEntity> getall() {
		List<DepartmentEntity> l=dep.findAll();
		return l;
	}
	
}
