package com.example.Review1.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.Review1.Entity.AcadamicYear;
import com.example.Review1.Repository.Acadamic_repo;

@Service
public class AcadamicService {
	
	@Autowired
	Acadamic_repo acadamy;

	public void addAcadamic(AcadamicYear aca) {
		acadamy.save(aca);
	}
}
