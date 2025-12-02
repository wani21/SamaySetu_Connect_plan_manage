package com.College.timetable.Entity;

import java.sql.Timestamp;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "students")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Student {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@NotBlank(message = "Student name is required")
	@Size(max = 100)
	@Column(nullable = false)
	private String name;
	
	@NotBlank(message = "Roll number is required")
	@Size(max = 20)
	@Column(name = "roll_number", nullable = false, unique = true)
	private String rollNumber;
	
	@Email(message = "Invalid email format")
	@Column(unique = true)
	private String email;
	
	@Size(max = 15)
	private String phone;
	
	@NotNull(message = "Admission year is required")
	@Column(name = "admission_year", nullable = false)
	private Integer admissionYear;
	
	@Column(name = "is_active")
	private Boolean isActive = true;
	
	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private Timestamp createdAt;
	
	@UpdateTimestamp
	@Column(name = "updated_at")
	private Timestamp updatedAt;
	
	// Relationships
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "division_id")
	private Division division;
}
