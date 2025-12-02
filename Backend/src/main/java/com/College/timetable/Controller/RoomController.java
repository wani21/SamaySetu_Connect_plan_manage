package com.College.timetable.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.College.timetable.Entity.ClassRoom;
import com.College.timetable.Service.RoomService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {
	
	@Autowired
	private RoomService roomService;

	@PostMapping
	public ResponseEntity<String> addRoom(@Valid @RequestBody ClassRoom room) {
		roomService.addRoom(room);
		return ResponseEntity.ok("Room added successfully");
	}
}
