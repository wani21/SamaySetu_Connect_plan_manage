package com.College.timetable.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.College.timetable.Entity.ClassRoom;
import com.College.timetable.Entity.DepartmentEntity;
import com.College.timetable.Repository.Dep_repo;
import com.College.timetable.Repository.Room_repo;

import jakarta.persistence.EntityNotFoundException;

@Service
public class RoomService {
	
	@Autowired
	private Room_repo rm;
	
	@Autowired
	private Dep_repo dep;

	public void addRoom(ClassRoom room) {
		// Validate department exists
		if (room.getDepartment() != null && room.getDepartment().getId() != null) {
			DepartmentEntity depart = dep.findById(room.getDepartment().getId())
				.orElseThrow(() -> new EntityNotFoundException("Department not found"));
		}
		rm.save(room);
	}
}
