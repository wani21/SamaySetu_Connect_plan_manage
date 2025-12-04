package com.College.timetable.Entity;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "courses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CourseEntity {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@NotBlank(message = "Course name is required")
	@Size(max = 100)
	@Column(name = "name", nullable = false)
	private String name;
	
	@NotBlank(message = "Course code is required")
	@Size(max = 20)
	@Column(name = "code", nullable = false, unique = true)
	private String code;
	
	@NotNull(message = "Course type is required")
	@Enumerated(EnumType.STRING)
	@Column(name = "course_type", nullable = false)
	private CourseType courseType;
	
	@Min(value = 1, message = "Credits must be at least 1")
	@Column(nullable = false)
	private Integer credits;
	
	@Min(value = 1, message = "Hours per week must be at least 1")
	@Column(name = "hours_per_week", nullable = false)
	private Integer hoursPerWeek;
	
	@NotNull(message = "Semester is required")
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Semester semester;
	
	@Column(columnDefinition = "TEXT")
	private String description;
	
	@Column(columnDefinition = "TEXT")
	private String prerequisites;
	
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
	@JoinColumn(name = "department_id", nullable = false)
	private DepartmentEntity department;
	
	@ManyToMany(mappedBy = "courses")
	@com.fasterxml.jackson.annotation.JsonIgnore
	private Set<TeacherEntity> teachers = new HashSet<>();
	
	@OneToMany(mappedBy = "course", cascade = CascadeType.ALL)
	@com.fasterxml.jackson.annotation.JsonIgnore
	private List<TimetableEntry> timetableEntries;
}
