package com.College.timetable.Entity;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

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
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TeacherEntity implements UserDetails{

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
	
	@Column(name = "is_approved")
	private Boolean isApproved = false;
	
	@NotBlank(message = "Password is required")
	private String password;

	@Column(name = "role")
	private String role="TEACHER";
	
	@Column(name = "is_email_verified")
	private Boolean isEmailVerified = false;
	
	@Column(name = "verification_token")
	private String verificationToken;
	
	@Column(name = "verification_token_expiry")
	private Timestamp verificationTokenExpiry;
	
	@Column(name = "password_reset_token")
	private String passwordResetToken;
	
	@Column(name = "password_reset_token_expiry")
	private Timestamp passwordResetTokenExpiry;
	
	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private Timestamp createdAt;
	
	@Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Spring Security needs ROLE_ prefix internally
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

	
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
	@com.fasterxml.jackson.annotation.JsonIgnore
	private Set<CourseEntity> courses = new HashSet<>();
	
	@OneToMany(mappedBy = "teacher", cascade = CascadeType.ALL)
	@com.fasterxml.jackson.annotation.JsonIgnore
	private List<TeacherAvailability> availabilities;
	
	@OneToMany(mappedBy = "teacher", cascade = CascadeType.ALL)
	@com.fasterxml.jackson.annotation.JsonIgnore
	private List<TimetableEntry> timetableEntries;

	@Override
	public String getUsername() {
		return email;
	}
}
