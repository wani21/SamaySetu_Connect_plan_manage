package com.College.timetable.Service;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.College.timetable.Entity.DepartmentEntity;
import com.College.timetable.Entity.TeacherEntity;
import com.College.timetable.Repository.Dep_repo;
import com.College.timetable.Repository.Teacher_Repo;

import jakarta.persistence.EntityNotFoundException;

@Service
public class TeacherService implements UserDetailsService{
	
	@Autowired
	private Teacher_Repo teacher;
	
	@Autowired
	private Dep_repo department;
	
	public void add(TeacherEntity teach) {
		// Validate department exists
		if (teach.getDepartment() != null && teach.getDepartment().getId() != null) {
			DepartmentEntity depart = department.findById(teach.getDepartment().getId())
				.orElseThrow(() -> new EntityNotFoundException("Department not found"));
		}
		teacher.save(teach);
	}
	
	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		
		TeacherEntity teach=teacher.findbyEmail(email).orElseThrow(()->new RuntimeException("The user is not found"));
		
		return new User(
				teach.getName(),
				teach.getEmail(),
				Collections.singleton(new SimpleGrantedAuthority(teach.getRole()))
				);
		
	}
	
	public String getByRole(String email) {
		TeacherEntity teach = teacher.findbyEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return teach.getRole(); 
    }

	
}
