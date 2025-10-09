package com.example.Review1.Conttoller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.Review1.Entity.ClassRoom;
import com.example.Review1.Service.RoomService;

@RestController
@RequestMapping("/room")
public class RoomController {
	
	@Autowired
	RoomService rm;

	@PostMapping("/add")
	public void addRoom(@RequestBody ClassRoom room) {
		rm.addRoom(room);
	}

}
