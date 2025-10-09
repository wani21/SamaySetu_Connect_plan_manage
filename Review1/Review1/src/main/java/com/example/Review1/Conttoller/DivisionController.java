package com.example.Review1.Conttoller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.Review1.Entity.Division;
import com.example.Review1.Service.DivisionService;

@RestController
@RequestMapping("/div")
public class DivisionController {
	
	@Autowired
	DivisionService div;
	
	@PostMapping("/add")
	public void addDiv(@RequestBody Division division) {
		div.addDivision(division);
	}
}
