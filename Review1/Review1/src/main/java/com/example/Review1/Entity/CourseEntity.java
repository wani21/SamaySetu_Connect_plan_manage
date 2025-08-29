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
import jakarta.persistence.Table;

@Entity
public class CourseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long courseId;
	
	@Column(nullable = false)
	private String courseName;
	
	@Column(nullable = false)
	private String courseCode;
	
	@Enumerated(EnumType.STRING)
	private CourseType courseType;
	
	@Column(nullable = false)
	private int Credits;
	
	@Column(nullable = false)
	private int Hours_per_week;
	
	@Enumerated(EnumType.STRING)
	private Semester Sem;
	
	@Column(nullable = false)
	private String Description;
	
	@CreationTimestamp
	private Timestamp created_at;
	
	@UpdateTimestamp
	private Timestamp updated_at;
	
	public CourseEntity() {
		
	}

	public CourseEntity(long courseId, String courseName, String courseCode, CourseType courseType, int credits,
			int hours_per_week, Semester sem, String description, Timestamp created_at, Timestamp updated_at) {
		super();
		this.courseId = courseId;
		this.courseName = courseName;
		this.courseCode = courseCode;
		this.courseType = courseType;
		Credits = credits;
		Hours_per_week = hours_per_week;
		Sem = sem;
		Description = description;
		this.created_at = created_at;
		this.updated_at = updated_at;
	}

	public long getCourseId() {
		return courseId;
	}

	public void setCourseId(long courseId) {
		this.courseId = courseId;
	}

	public String getCourseName() {
		return courseName;
	}

	public void setCourseName(String courseName) {
		this.courseName = courseName;
	}

	public String getCourseCode() {
		return courseCode;
	}

	public void setCourseCode(String courseCode) {
		this.courseCode = courseCode;
	}

	public CourseType getCourseType() {
		return courseType;
	}

	public void setCourseType(CourseType courseType) {
		this.courseType = courseType;
	}

	public int getCredits() {
		return Credits;
	}

	public void setCredits(int credits) {
		Credits = credits;
	}

	public int getHours_per_week() {
		return Hours_per_week;
	}

	public void setHours_per_week(int hours_per_week) {
		Hours_per_week = hours_per_week;
	}

	public Semester getSem() {
		return Sem;
	}

	public void setSem(Semester sem) {
		Sem = sem;
	}

	public String getDescription() {
		return Description;
	}

	public void setDescription(String description) {
		Description = description;
	}

	public Timestamp getCreated_at() {
		return created_at;
	}

	public void setCreated_at(Timestamp created_at) {
		this.created_at = created_at;
	}

	public Timestamp getUpdated_at() {
		return updated_at;
	}

	public void setUpdated_at(Timestamp updated_at) {
		this.updated_at = updated_at;
	}

	@Override
	public String toString() {
		return "CourseEntity [courseId=" + courseId + ", courseName=" + courseName + ", courseCode=" + courseCode
				+ ", courseType=" + courseType + ", Credits=" + Credits + ", Hours_per_week=" + Hours_per_week
				+ ", Sem=" + Sem + ", Description=" + Description + ", created_at=" + created_at + ", updated_at="
				+ updated_at + "]";
	}
	
	
	
}



