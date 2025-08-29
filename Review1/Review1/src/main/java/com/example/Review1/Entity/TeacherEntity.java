package com.example.Review1.Entity;

import java.sql.Timestamp;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class TeacherEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long teacherId;
	
	@Column(nullable = false)
	private String teacherName;
	
	@Column(unique = true,nullable = false)
	private String email;
	
	@Column(unique = true,nullable = false)
	private long phone;
	
	private int Weekly_hours_limit=25;
	
	@CreationTimestamp
	private Timestamp created_at;
	
	@UpdateTimestamp
	private Timestamp Updated_at;
	
	
    private long department;
 
    private long course;
    
    public TeacherEntity() {
    	
    }

	public TeacherEntity(long teacherId, String teacherName, String email, long phone, int weekly_hours_limit,
			Timestamp created_at, Timestamp updated_at, long department, long course) {
		super();
		this.teacherId = teacherId;
		this.teacherName = teacherName;
		this.email = email;
		this.phone = phone;
		Weekly_hours_limit = weekly_hours_limit;
		this.created_at = created_at;
		Updated_at = updated_at;
		this.department = department;
		this.course = course;
	}

	public long getTeacherId() {
		return teacherId;
	}

	public void setTeacherId(long teacherId) {
		this.teacherId = teacherId;
	}

	public String getTeacherName() {
		return teacherName;
	}

	public void setTeacherName(String teacherName) {
		this.teacherName = teacherName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public long getPhone() {
		return phone;
	}

	public void setPhone(long phone) {
		this.phone = phone;
	}

	public int getWeekly_hours_limit() {
		return Weekly_hours_limit;
	}

	public void setWeekly_hours_limit(int weekly_hours_limit) {
		Weekly_hours_limit = weekly_hours_limit;
	}

	public Timestamp getCreated_at() {
		return created_at;
	}

	public void setCreated_at(Timestamp created_at) {
		this.created_at = created_at;
	}

	public Timestamp getUpdated_at() {
		return Updated_at;
	}

	public void setUpdated_at(Timestamp updated_at) {
		Updated_at = updated_at;
	}

	public long getDepartment() {
		return department;
	}

	public void setDepartment(long department) {
		this.department = department;
	}

	public long getCourse() {
		return course;
	}

	public void setCourse(long course) {
		this.course = course;
	}

	@Override
	public String toString() {
		return "TeacherEntity [teacherId=" + teacherId + ", teacherName=" + teacherName + ", email=" + email
				+ ", phone=" + phone + ", Weekly_hours_limit=" + Weekly_hours_limit + ", created_at=" + created_at
				+ ", Updated_at=" + Updated_at + ", department=" + department + ", course=" + course + "]";
	}

    
}
