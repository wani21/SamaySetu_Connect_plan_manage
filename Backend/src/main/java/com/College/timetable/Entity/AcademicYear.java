package com.College.timetable.Entity;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "academic_years")
@Data
@NoArgsConstructor
@AllArgsConstructor
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AcademicYear {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@NotBlank(message = "Year name is required")
	@Size(max = 20)
	@Column(name = "year_name", nullable = false, unique = true)
	private String yearName;
	
	@NotNull(message = "Start date is required")
	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;
	
	@NotNull(message = "End date is required")
	@Column(name = "end_date", nullable = false)
	private LocalDate endDate;
	
	@Column(name = "is_current")
	private Boolean isCurrent = false;
	
	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private Timestamp createdAt;
	
	// Relationships
	@OneToMany(mappedBy = "academicYear", cascade = CascadeType.ALL)
	@com.fasterxml.jackson.annotation.JsonIgnore
	private List<Division> divisions;
	
	@OneToMany(mappedBy = "academicYear", cascade = CascadeType.ALL)
	@com.fasterxml.jackson.annotation.JsonIgnore
	private List<TimetableEntry> timetableEntries;
}
