package com.College.timetable.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.College.timetable.Entity.AcademicYear;
import com.College.timetable.Repository.Acadamic_repo;

@Service
public class AcadamicService {
	
	@Autowired
	private Acadamic_repo acadamy;

	public void addAcadamic(AcademicYear aca) {
		acadamy.save(aca);
	}
}
