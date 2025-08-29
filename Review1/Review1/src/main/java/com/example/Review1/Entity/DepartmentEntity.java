package com.example.Review1.Entity;

import java.sql.Timestamp;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class DepartmentEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long departmentId;
	
	@Column(nullable = false)
	private String department_name;
	
	@Column(nullable=false)
	private String hod;
	
	@CreationTimestamp
	private Timestamp created_at;
	
	@UpdateTimestamp
	private Timestamp updated_at;

	
	public DepartmentEntity() {
		
	}
	public DepartmentEntity(long departmentId, String department_name, String hod, Timestamp created_at,
			Timestamp updated_at) {
		super();
		this.departmentId = departmentId;
		this.department_name = department_name;
		this.hod = hod;
		this.created_at = created_at;
		this.updated_at = updated_at;
	}

	public long getDepartmentId() {
		return departmentId;
	}

	public void setDepartmentId(long departmentId) {
		this.departmentId = departmentId;
	}

	public String getDepartment_name() {
		return department_name;
	}

	public void setDepartment_name(String department_name) {
		this.department_name = department_name;
	}

	public String getHod() {
		return hod;
	}

	public void setHod(String hod) {
		this.hod = hod;
	}

	public Timestamp getCreated_at() {
		return created_at;
	}

	public void setCreated_at(Timestamp created_at) {
		this.created_at = created_at;
	}

	public Timestamp getUpdated_at() {
		return updated_at;
	}

	public void setUpdated_at(Timestamp updated_at) {
		this.updated_at = updated_at;
	}

	@Override
	public String toString() {
		return "DepartmentEntity [departmentId=" + departmentId + ", department_name=" + department_name + ", hod="
				+ hod + ", created_at=" + created_at + ", updated_at=" + updated_at + "]";
	}

	
	
}


