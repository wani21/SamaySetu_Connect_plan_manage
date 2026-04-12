package com.College.timetable.Entity;

import java.sql.Timestamp;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "divisions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Division {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank(message = "Division name is required")
	@Size(max = 10)
	@Column(nullable = false)
	private String name;

	@NotNull(message = "Year is required")
	@Min(value = 1, message = "Year must be between 1 and 4")
	@Max(value = 4, message = "Year must be between 1 and 4")
	@Column(nullable = false)
	private Integer year;

	@NotBlank(message = "Branch is required")
	@Size(max = 50)
	@Column(nullable = false)
	private String branch;

	@Min(value = 0, message = "Total students cannot be negative")
	@Column(name = "total_students")
	private Integer totalStudents = 0;

	@Column(name = "is_active")
	private Boolean isActive = true;

	@Size(max = 20)
	@Column(name = "time_slot_type")
	private String timeSlotType = "TYPE_1";

	@Size(max = 100)
	@Column(name = "class_teacher")
	private String classTeacher;

	@Size(max = 100)
	@Column(name = "class_representative")
	private String classRepresentative;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private Timestamp createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private Timestamp updatedAt;

	// Relationships
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "department_id", nullable = false)
	@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"teachers", "courses", "rooms", "divisions"})
	private DepartmentEntity department;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "academic_year_id", nullable = false)
	@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"divisions", "timetableEntries", "departments"})
	private AcademicYear academicYear;

	@OneToMany(mappedBy = "division", cascade = CascadeType.ALL)
	@com.fasterxml.jackson.annotation.JsonIgnore
	private List<TimetableEntry> timetableEntries;

	@OneToMany(mappedBy = "division", cascade = CascadeType.ALL)
	@com.fasterxml.jackson.annotation.JsonIgnore
	private List<Student> students;
}
