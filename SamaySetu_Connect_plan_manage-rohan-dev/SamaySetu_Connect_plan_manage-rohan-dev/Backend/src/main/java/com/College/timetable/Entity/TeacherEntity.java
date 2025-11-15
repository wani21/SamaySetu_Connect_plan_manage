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
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "teachers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@NotBlank(message = "Teacher name is required")
	@Size(max = 100)
	@Column(name = "name", nullable = false)
	private String name;
	
	@NotBlank(message = "Employee ID is required")
	@Size(max = 20)
	@Column(name = "employee_id", unique = true, nullable = false)
	private String employeeId;
	
	@Email(message = "Invalid email format")
	@Column(unique = true)
	private String email;
	
	@Size(max = 15)
	private String phone;
	
	@Min(value = 1, message = "Weekly hours must be at least 1")
	@Max(value = 40, message = "Weekly hours cannot exceed 40")
	@Column(name = "weekly_hours_limit")
	private Integer weeklyHoursLimit = 25;
	
	@Column(columnDefinition = "TEXT")
	private String specialization;
	
	@Column(name = "is_active")
	private Boolean isActive = true;
	
	@NotBlank(message = "Password is required")
	private String password;

	@Column(name = "role")
	private String role="ROLE_TEACHER";
	
	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private Timestamp createdAt;
	
	@UpdateTimestamp
	@Column(name = "updated_at")
	private Timestamp updatedAt;
	
	// Relationships
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "department_id")
	private DepartmentEntity department;
	
	@ManyToMany
	@JoinTable(
		name = "teacher_courses",
		joinColumns = @JoinColumn(name = "teacher_id"),
		inverseJoinColumns = @JoinColumn(name = "course_id")
	)
	private Set<CourseEntity> courses = new HashSet<>();
	
	@OneToMany(mappedBy = "teacher", cascade = CascadeType.ALL)
	private List<TeacherAvailability> availabilities;
	
	@OneToMany(mappedBy = "teacher", cascade = CascadeType.ALL)
	private List<TimetableEntry> timetableEntries;
}
