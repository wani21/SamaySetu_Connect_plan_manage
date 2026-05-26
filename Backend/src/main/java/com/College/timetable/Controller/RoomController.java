package com.College.timetable.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.College.timetable.Entity.ClassRoom;
import com.College.timetable.Service.RoomService;
import com.College.timetable.Service.DepartmentAuthorizationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin/api/rooms")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEPARTMENT_ADMIN', 'HOD', 'TIMETABLE_COORDINATOR')")
public class RoomController {
	
	@Autowired
	private RoomService roomService;

	@Autowired
	private DepartmentAuthorizationService authService;

	@PostMapping
	public ResponseEntity<ClassRoom> addRoom(@Valid @RequestBody ClassRoom room) {
		if (room.getDepartment() != null && room.getDepartment().getId() != null) {
			authService.checkDepartmentAccess(room.getDepartment().getId());
		} else {
			authService.checkSuperAdminAccess();
		}
		ClassRoom saved = roomService.addRoom(room);
		return ResponseEntity.ok(saved);
	}
	
	@GetMapping
	public ResponseEntity<List<ClassRoom>> getAllRooms() {
		List<ClassRoom> all = roomService.getAll();
		if (authService.isInstitutionalAdmin()) {
			return ResponseEntity.ok(all);
		}
		Long deptId = authService.getCurrentUser().getDepartment().getId();
		List<ClassRoom> filtered = all.stream()
			.filter(r -> r.getDepartment() != null && deptId.equals(r.getDepartment().getId()))
			.toList();
		return ResponseEntity.ok(filtered);
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<ClassRoom> getRoomById(@PathVariable Long id) {
		authService.checkRoomAccess(id);
		return ResponseEntity.ok(roomService.getById(id));
	}
	
	@PutMapping("/{id}")
	public ResponseEntity<ClassRoom> updateRoom(@PathVariable Long id, @Valid @RequestBody ClassRoom room) {
		authService.checkRoomAccess(id);
		if (room.getDepartment() != null && room.getDepartment().getId() != null) {
			authService.checkDepartmentAccess(room.getDepartment().getId());
		}
		ClassRoom updated = roomService.update(id, room);
		return ResponseEntity.ok(updated);
	}
	
	@DeleteMapping("/{id}")
	public ResponseEntity<String> deleteRoom(@PathVariable Long id) {
		authService.checkRoomAccess(id);
		roomService.delete(id);
		return ResponseEntity.ok("Room deleted successfully");
	}
}
