package com.College.timetable.Entity;

import java.sql.Timestamp;
import java.time.LocalTime;

import org.hibernate.annotations.CreationTimestamp;

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
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "teacher_availability")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherAvailability {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@NotNull(message = "Teacher is required")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "teacher_id", nullable = false)
	private TeacherEntity teacher;
	
	@NotNull(message = "Day of week is required")
	@Enumerated(EnumType.STRING)
	@Column(name = "day_of_week", nullable = false)
	private DayOfWeek dayOfWeek;
	
	@NotNull(message = "Start time is required")
	@Column(name = "start_time", nullable = false)
	private LocalTime startTime;
	
	@NotNull(message = "End time is required")
	@Column(name = "end_time", nullable = false)
	private LocalTime endTime;
	
	@Column(name = "is_available")
	private Boolean isAvailable = true;
	
	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private Timestamp createdAt;
}
