package com.example.Review1.Entity;

import java.sql.Timestamp;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class ClassRoom {
	
//	id INT PRIMARY KEY AUTO_INCREMENT,
//    name VARCHAR(50) NOT NULL UNIQUE,
//    room_number VARCHAR(20) NOT NULL UNIQUE,
//    capacity INT NOT NULL,
//    room_type ENUM('classroom', 'lab', 'auditorium') NOT NULL,
//    department_id INT,
//    has_projector TINYINT(1) DEFAULT 0,
//    has_ac TINYINT(1) DEFAULT 0,
//    equipment TEXT,
//    is_active TINYINT(1) DEFAULT 1,
//    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
//    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long roomId;
	
	
	@Column(nullable = false)
	private String roomName;
	
	@Column(nullable = false)
	private int roomNumber;
	
	@Column(nullable = false)
	private int Capacity;
	
	@Enumerated(EnumType.STRING)
	private RoomType roomType;
	
	@Column(nullable = false)
	private int department_id;
	
	@Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
	private boolean has_projector;
	
	
	@Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
	private boolean has_ac;
	
	@CreationTimestamp
	@Column(updatable = false)
	private Timestamp created_at;
	
	@UpdateTimestamp
	private Timestamp updated_at;
	
	
	
}
