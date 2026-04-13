package com.College.timetable.Entity;

import java.sql.Timestamp;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.College.timetable.Service.TimetableService;

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
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "division_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"timetableEntries", "students", "hibernateLazyInitializer", "handler"})
    private Division division;

    @NotNull(message = "Course is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"timetableEntries", "teachers", "hibernateLazyInitializer", "handler"})
    private CourseEntity course;

    @NotNull(message = "Teacher is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"timetableEntries", "availabilities", "courses", "hibernateLazyInitializer", "handler"})
    private TeacherEntity teacher;

    @NotNull(message = "Room is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "classroom_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"timetableEntries", "hibernateLazyInitializer", "handler"})
    private ClassRoom room;

    @NotNull(message = "Time slot is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "time_slot_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"timetableEntries", "hibernateLazyInitializer", "handler"})
    private TimeSlot timeSlot;

    @NotNull(message = "Day of week is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    @NotNull(message = "Academic year is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "academic_year_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"divisions", "timetableEntries", "hibernateLazyInitializer", "handler"})
    private AcademicYear academicYear;

    // --- NEW FIELDS ---

    // DRAFT = being built, PUBLISHED = live for students, ARCHIVED = old semester
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private TimetableStatus status = TimetableStatus.DRAFT;

    // Semester this entry belongs to (e.g. SEM_3)
    @Enumerated(EnumType.STRING)
    @Column(name = "semester")
    private Semester semester;

    // True if this is a lab session entry (part of a lab_session_group)
    @Column(name = "is_lab_session")
    private Boolean isLabSession = false;

    // For lab sessions — which batch this entry is for (nullable for theory)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "batch_id")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"division", "hibernateLazyInitializer", "handler"})
    private Batch batch;

    // Groups all parallel batch entries for the same lab session
    // If this is NOT NULL, the entry is a lab entry and skips teacher/division uniqueness check
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "lab_session_group_id")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"entries", "division", "course", "academicYear", "timeSlot", "createdBy", "hibernateLazyInitializer", "handler"})
    private LabSessionGroup labSessionGroup;

    // --- EXISTING FIELDS ---

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
