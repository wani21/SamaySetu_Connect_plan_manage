package com.College.timetable.Entity;

import java.sql.Timestamp;
import java.time.LocalDate;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class AcadamicYear {
	
//	id INT PRIMARY KEY AUTO_INCREMENT,
//    year_name VARCHAR(20) NOT NULL UNIQUE COMMENT 'e.g., 2024-25',
//    start_date DATE NOT NULL,
//    end_date DATE NOT NULL,
//    is_current TINYINT(1) DEFAULT 0,
//    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
	

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long AcadaminId;
	
	private String yearName;
	
	private LocalDate start_date;
	
	private LocalDate end_date;
	
	@Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private boolean is_active = true;
	
	@CreationTimestamp
	private Timestamp created_at;
	
	
}
