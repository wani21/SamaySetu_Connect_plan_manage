package com.College.timetable.Service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.College.timetable.Entity.DepartmentEntity;
import com.College.timetable.Repository.Dep_repo;

@Service
public class DepartmentService {
	
	@Autowired
	private Dep_repo dep;
	
	public void addDep(DepartmentEntity d) {
		dep.save(d);
	}
	
	public List<DepartmentEntity> getall() {
		return dep.findAll();
	}
}
