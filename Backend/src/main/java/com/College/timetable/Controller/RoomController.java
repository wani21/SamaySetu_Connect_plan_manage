package com.College.timetable.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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

import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin/api/rooms")
public class RoomController {
	
	@Autowired
	private RoomService roomService;

	@PostMapping
	public ResponseEntity<ClassRoom> addRoom(@Valid @RequestBody ClassRoom room) {
		ClassRoom saved = roomService.addRoom(room);
		return ResponseEntity.ok(saved);
	}
	
	@GetMapping
	public ResponseEntity<List<ClassRoom>> getAllRooms() {
		return ResponseEntity.ok(roomService.getAll());
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<ClassRoom> getRoomById(@PathVariable Long id) {
		return ResponseEntity.ok(roomService.getById(id));
	}
	
	@PutMapping("/{id}")
	public ResponseEntity<ClassRoom> updateRoom(@PathVariable Long id, @Valid @RequestBody ClassRoom room) {
		ClassRoom updated = roomService.update(id, room);
		return ResponseEntity.ok(updated);
	}
	
	@DeleteMapping("/{id}")
	public ResponseEntity<String> deleteRoom(@PathVariable Long id) {
		roomService.delete(id);
		return ResponseEntity.ok("Room deleted successfully");
	}
}
