package com.College.timetable.Entity;

import java.sql.Timestamp;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "timetable_entries")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimetableEntry {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@NotNull(message = "Division is required")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "division_id", nullable = false)
	private Division division;
	
	@NotNull(message = "Course is required")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "course_id", nullable = false)
	private CourseEntity course;
	
	@NotNull(message = "Teacher is required")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "teacher_id", nullable = false)
	private TeacherEntity teacher;
	
	@NotNull(message = "Room is required")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "room_id", nullable = false)
	private ClassRoom room;
	
	@NotNull(message = "Time slot is required")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "time_slot_id", nullable = false)
	private TimeSlot timeSlot;
	
	@NotNull(message = "Day of week is required")
	@Enumerated(EnumType.STRING)
	@Column(name = "day_of_week", nullable = false)
	private DayOfWeek dayOfWeek;
	
	@NotNull(message = "Academic year is required")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "academic_year_id", nullable = false)
	private AcademicYear academicYear;
	
	@Min(value = 1, message = "Week number must be at least 1")
	@Column(name = "week_number")
	private Integer weekNumber = 1;
	
	@Column(name = "is_recurring")
	private Boolean isRecurring = true;
	
	@Column(columnDefinition = "TEXT")
	private String notes;
	
	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private Timestamp createdAt;
	
	@UpdateTimestamp
	@Column(name = "updated_at")
	private Timestamp updatedAt;
}
