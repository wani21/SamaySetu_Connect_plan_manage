package com.example.Review1.Conttoller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.Review1.Entity.AcadamicYear;
import com.example.Review1.Service.AcadamicService;

@RestController
@RequestMapping("/acadamic")
public class AcadamicController {
	
	@Autowired
	AcadamicService acadamy;
	
	
	
	@PostMapping("/add")
	public void addAca(@RequestBody AcadamicYear aca) {
		acadamy.addAcadamic(aca);
	}
}
