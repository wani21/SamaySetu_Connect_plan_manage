package com.College.timetable.Entity;

import java.sql.Timestamp;
import java.util.List;

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
@Table(name = "classrooms")
@Data
@NoArgsConstructor
@AllArgsConstructor
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ClassRoom {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@NotBlank(message = "Room name is required")
	@Size(max = 50)
	@Column(name = "name", nullable = false, unique = true)
	private String name;
	
	@NotBlank(message = "Room number is required")
	@Size(max = 20)
	@Column(name = "room_number", nullable = false, unique = true)
	private String roomNumber;
	
	@NotBlank(message = "Building wing is required")
	@Size(max = 10)
	@Column(name = "building_wing", nullable = false)
	private String buildingWing;
	
	@Min(value = 1, message = "Capacity must be at least 1")
	@Column(nullable = false)
	private Integer capacity;
	
	@NotNull(message = "Room type is required")
	@Enumerated(EnumType.STRING)
	@Column(name = "room_type", nullable = false)
	private RoomType roomType;
	
	@Column(name = "has_projector")
	private Boolean hasProjector = false;
	
	@Column(name = "has_ac")
	private Boolean hasAc = false;
	
	@Column(columnDefinition = "TEXT")
	private String equipment;
	
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
	@JoinColumn(name = "department_id")
	private DepartmentEntity department;
	
	@OneToMany(mappedBy = "room", cascade = CascadeType.ALL)
	@com.fasterxml.jackson.annotation.JsonIgnore
	private List<TimetableEntry> timetableEntries;
}
