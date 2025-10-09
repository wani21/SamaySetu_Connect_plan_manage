package com.example.Review1.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.Review1.Entity.AcadamicYear;
import com.example.Review1.Entity.Division;
import com.example.Review1.Repository.Acadamic_repo;
import com.example.Review1.Repository.Division_repo;

import jakarta.persistence.EntityNotFoundException;

@Service
public class DivisionService {
	
	
	@Autowired
	Division_repo div;
	
	@Autowired
	Acadamic_repo aca;
	
	public void addDivision(Division division) {
		AcadamicYear a=aca.findById((long) division.getAcademicYearId()).orElseThrow(()-> new EntityNotFoundException("The acadamic id is not found"));
		div.save(division);
	}

}
