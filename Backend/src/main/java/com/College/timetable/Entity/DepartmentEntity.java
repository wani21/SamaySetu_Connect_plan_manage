package com.College.timetable.Entity;

import java.sql.Timestamp;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "departments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DepartmentEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@NotBlank(message = "Department name is required")
	@Size(max = 100)
	@Column(name = "name", nullable = false, unique = true)
	private String name;
	
	@NotBlank(message = "Department code is required")
	@Size(max = 10)
	@Column(name = "code", nullable = false, unique = true)
	private String code;
	
	@Size(max = 100)
	@Column(name = "head_of_department")
	private String headOfDepartment;
	
	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private Timestamp createdAt;
	
	@UpdateTimestamp
	@Column(name = "updated_at")
	private Timestamp updatedAt;
	
	// Relationships
	@OneToMany(mappedBy = "department", cascade = CascadeType.ALL)
	@com.fasterxml.jackson.annotation.JsonIgnore
	private List<TeacherEntity> teachers;
	
	@OneToMany(mappedBy = "department", cascade = CascadeType.ALL)
	@com.fasterxml.jackson.annotation.JsonIgnore
	private List<CourseEntity> courses;
	
	@OneToMany(mappedBy = "department", cascade = CascadeType.ALL)
	@com.fasterxml.jackson.annotation.JsonIgnore
	private List<ClassRoom> rooms;
	
	@OneToMany(mappedBy = "department", cascade = CascadeType.ALL)
	@com.fasterxml.jackson.annotation.JsonIgnore
	private List<Division> divisions;
}


