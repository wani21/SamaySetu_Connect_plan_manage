package com.College.timetable.Service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

	@Transactional
	public ClassRoom addRoom(ClassRoom room) {
		// Validate department exists
		if (room.getDepartment() != null && room.getDepartment().getId() != null) {
			DepartmentEntity depart = dep.findById(room.getDepartment().getId())
				.orElseThrow(() -> new EntityNotFoundException("Department not found"));
		}
		return rm.save(room);
	}

	@Transactional(readOnly = true)
	public List<ClassRoom> getAll() {
		return rm.findAll();
	}

	@Transactional(readOnly = true)
	public ClassRoom getById(Long id) {
		return rm.findById(id)
			.orElseThrow(() -> new EntityNotFoundException("Room not found with id: " + id));
	}

	@Transactional
	public ClassRoom update(Long id, ClassRoom room) {
		ClassRoom existing = getById(id);
		existing.setName(room.getName());
		existing.setRoomNumber(room.getRoomNumber());
		existing.setCapacity(room.getCapacity());
		existing.setRoomType(room.getRoomType());
		existing.setHasProjector(room.getHasProjector());
		existing.setHasAc(room.getHasAc());
		existing.setEquipment(room.getEquipment());
		existing.setIsActive(room.getIsActive());

		// Update department if provided
		if (room.getDepartment() != null && room.getDepartment().getId() != null) {
			DepartmentEntity depart = dep.findById(room.getDepartment().getId())
				.orElseThrow(() -> new EntityNotFoundException("Department not found"));
			existing.setDepartment(depart);
		}

		return rm.save(existing);
	}

	@Transactional
	public void delete(Long id) {
		if (!rm.existsById(id)) {
			throw new EntityNotFoundException("Room not found with id: " + id);
		}
		rm.deleteById(id);
	}
}
