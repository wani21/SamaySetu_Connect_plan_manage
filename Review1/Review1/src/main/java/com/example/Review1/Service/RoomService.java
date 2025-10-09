package com.example.Review1.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.Review1.Entity.ClassRoom;
import com.example.Review1.Entity.DepartmentEntity;
import com.example.Review1.Repository.Dep_repo;
import com.example.Review1.Repository.Room_repo;

import jakarta.persistence.EntityNotFoundException;

@Service
public class RoomService {
	
	
	@Autowired
	Room_repo rm;
	
	@Autowired
	Dep_repo dep;

	public void addRoom(ClassRoom room) {
		DepartmentEntity depart=dep.findById((long) room.getDepartment_id()).orElseThrow(() -> new EntityNotFoundException("The department id is not found"));
		rm.save(room);
	}

}
