package com.example.Review1.Entity;

import java.sql.Timestamp;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Data;

@Entity
@Data
public class Division {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;
	
	@Column(nullable=false)
	private String name;
	
	@Column(nullable=false)
	private int year;
	
	@Column(nullable=false)
	private String branch;
	
	@Column(nullable=false)
	private int departmentId;
	
	@Column(nullable=false)
	private int academicYearId;
	
	@Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private boolean is_active = true;
	
	@Column(nullable=false)
	private int total_students;
	
	@CreationTimestamp
    @Column(updatable = false)
	private Timestamp created_at;
	
	@UpdateTimestamp
	private Timestamp updated_at;
	
	
}
