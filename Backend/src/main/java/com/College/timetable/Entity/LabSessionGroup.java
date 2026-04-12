package com.College.timetable.Entity;


import java.sql.Timestamp;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

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
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Groups all parallel batch entries for one lab session.
 *
 * Example: CS 3rd Year Div A has a DBMS lab on Monday P3-P4.
 * Division splits into Batch A, B, C — each goes to a different lab with a different teacher.
 * All three TimetableEntry rows share the same LabSessionGroup.
 * The conflict checker sees the group and knows these are intentional parallels, not conflicts.
 */
@Entity
@Table(name = "lab_session_groups")
@Data
@NoArgsConstructor
@AllArgsConstructor
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class LabSessionGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "division_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"timetableEntries", "students", "hibernateLazyInitializer", "handler"})
    private Division division;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"timetableEntries", "teachers", "hibernateLazyInitializer", "handler"})
    private CourseEntity course;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "academic_year_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"divisions", "timetableEntries", "hibernateLazyInitializer", "handler"})
    private AcademicYear academicYear;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "time_slot_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"timetableEntries", "hibernateLazyInitializer", "handler"})
    private TimeSlot timeSlot;

    @Enumerated(EnumType.STRING)
    @Column(name = "semester")
    private Semester semester;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"timetableEntries", "availabilities", "courses", "hibernateLazyInitializer", "handler"})
    private TeacherEntity createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Timestamp createdAt;

    // All batch entries belonging to this lab session
    @OneToMany(mappedBy = "labSessionGroup", cascade = CascadeType.ALL)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<TimetableEntry> entries;
}
