package com.College.timetable.Entity;

import java.sql.Timestamp;
import java.time.LocalTime;
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
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "time_slots")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlot {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@NotNull(message = "Start time is required")
	@Column(name = "start_time", nullable = false)
	private LocalTime startTime;
	
	@NotNull(message = "End time is required")
	@Column(name = "end_time", nullable = false)
	private LocalTime endTime;
	
	@Min(value = 1, message = "Duration must be at least 1 minute")
	@Column(name = "duration_minutes", nullable = false)
	private Integer durationMinutes;
	
	@Size(max = 50)
	@Column(name = "slot_name")
	private String slotName;
	
	@Column(name = "is_break")
	private Boolean isBreak = false;
	
	@Column(name = "is_active")
	private Boolean isActive = true;
	
	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private Timestamp createdAt;
	
	// Relationships
	@OneToMany(mappedBy = "timeSlot", cascade = CascadeType.ALL)
	private List<TimetableEntry> timetableEntries;
}
