# Enhanced Features Roadmap - College Timetable Management System

## 📋 Overview
This document addresses the gaps identified in the development workflow and provides implementation strategies for real-world educator needs.

---

## 🔴 HIGH PRIORITY FEATURES

### 1. Elective & Batch Handling
**Problem:** Multiple divisions sharing labs/electives creates complex scheduling scenarios

**Database Schema Additions:**

```sql
-- Elective Groups Table
CREATE TABLE elective_groups (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    academic_year_id INT NOT NULL,
    semester ENUM('1','2','3','4','5','6','7','8') NOT NULL,
    max_students_per_elective INT DEFAULT 30,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_elective_academic FOREIGN KEY (academic_year_id) 
        REFERENCES academic_years(id) ON DELETE CASCADE,
    INDEX idx_elective_semester (semester)
);

-- Elective Courses Mapping
CREATE TABLE elective_courses (
    id INT PRIMARY KEY AUTO_INCREMENT,
    elective_group_id INT NOT NULL,
    course_id INT NOT NULL,
    max_capacity INT DEFAULT 30,
    
    CONSTRAINT fk_elective_group FOREIGN KEY (elective_group_id) 
        REFERENCES elective_groups(id) ON DELETE CASCADE,
    CONSTRAINT fk_elective_course FOREIGN KEY (course_id) 
        REFERENCES courses(id) ON DELETE CASCADE,
    UNIQUE KEY uk_elective_course (elective_group_id, course_id)
);

-- Student Elective Enrollment
CREATE TABLE student_electives (
    id INT PRIMARY KEY AUTO_INCREMENT,
    student_id INT NOT NULL,
    elective_course_id INT NOT NULL,
    enrollment_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_student_elective FOREIGN KEY (student_id) 
        REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT fk_elective_enrollment FOREIGN KEY (elective_course_id) 
        REFERENCES elective_courses(id) ON DELETE CASCADE,
    UNIQUE KEY uk_student_elective (student_id, elective_course_id)
);

-- Batch/Lab Groups (for splitting divisions)
CREATE TABLE lab_batches (
    id INT PRIMARY KEY AUTO_INCREMENT,
    division_id INT NOT NULL,
    batch_name VARCHAR(10) NOT NULL COMMENT 'Batch A, Batch B',
    student_count INT DEFAULT 0,
    
    CONSTRAINT fk_batch_division FOREIGN KEY (division_id) 
        REFERENCES divisions(id) ON DELETE CASCADE,
    UNIQUE KEY uk_division_batch (division_id, batch_name)
);
```

**Java Entity Implementation:**

```java
@Entity
@Table(name = "elective_groups")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ElectiveGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    private String name;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    
    @Enumerated(EnumType.STRING)
    private Semester semester;
    
    @Column(name = "max_students_per_elective")
    private Integer maxStudentsPerElective = 30;
    
    @OneToMany(mappedBy = "electiveGroup", cascade = CascadeType.ALL)
    private List<ElectiveCourse> electiveCourses;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
}

@Entity
@Table(name = "lab_batches")
@Data
public class LabBatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "division_id", nullable = false)
    private Division division;
    
    @NotBlank
    @Column(name = "batch_name")
    private String batchName;
    
    @Column(name = "student_count")
    private Integer studentCount = 0;
}
```

**API Endpoints:**
```
POST   /api/electives/groups              - Create elective group
GET    /api/electives/groups/{id}         - Get elective details
POST   /api/electives/enroll              - Student enrollment
GET    /api/batches/division/{id}         - Get lab batches for division
POST   /api/timetable/batch-schedule      - Schedule for specific batch
```

---

### 2. Holidays & Exam Management
**Problem:** Need academic calendar integration and automatic rescheduling


**Database Schema:**

```sql
-- Academic Calendar Events
CREATE TABLE academic_events (
    id INT PRIMARY KEY AUTO_INCREMENT,
    academic_year_id INT NOT NULL,
    event_type ENUM('holiday', 'exam', 'vacation', 'workshop', 'other') NOT NULL,
    event_name VARCHAR(200) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    affects_timetable TINYINT(1) DEFAULT 1,
    description TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_event_academic FOREIGN KEY (academic_year_id) 
        REFERENCES academic_years(id) ON DELETE CASCADE,
    INDEX idx_event_dates (start_date, end_date),
    INDEX idx_event_type (event_type)
);

-- Exam Schedule
CREATE TABLE exam_schedule (
    id INT PRIMARY KEY AUTO_INCREMENT,
    course_id INT NOT NULL,
    division_id INT NOT NULL,
    exam_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    room_id INT NOT NULL,
    exam_type ENUM('mid_term', 'end_term', 'practical', 'viva') NOT NULL,
    academic_year_id INT NOT NULL,
    
    CONSTRAINT fk_exam_course FOREIGN KEY (course_id) 
        REFERENCES courses(id) ON DELETE CASCADE,
    CONSTRAINT fk_exam_division FOREIGN KEY (division_id) 
        REFERENCES divisions(id) ON DELETE CASCADE,
    CONSTRAINT fk_exam_room FOREIGN KEY (room_id) 
        REFERENCES rooms(id) ON DELETE CASCADE,
    INDEX idx_exam_date (exam_date)
);


-- Timetable Adjustments (for rescheduling)
CREATE TABLE timetable_adjustments (
    id INT PRIMARY KEY AUTO_INCREMENT,
    original_entry_id INT NOT NULL,
    adjustment_date DATE NOT NULL,
    new_time_slot_id INT,
    new_room_id INT,
    new_teacher_id INT,
    reason VARCHAR(500),
    status ENUM('pending', 'approved', 'rejected') DEFAULT 'pending',
    created_by INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_adjustment_entry FOREIGN KEY (original_entry_id) 
        REFERENCES timetable_entries(id) ON DELETE CASCADE,
    INDEX idx_adjustment_date (adjustment_date),
    INDEX idx_adjustment_status (status)
);
```

**Java Implementation:**

```java
@Entity
@Table(name = "academic_events")
@Data
public class AcademicEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "academic_year_id")
    private AcademicYear academicYear;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type")
    private EventType eventType;
    
    @NotBlank
    @Column(name = "event_name")
    private String eventName;
    
    @NotNull
    private LocalDate startDate;
    
    @NotNull
    private LocalDate endDate;
    
    @Column(name = "affects_timetable")
    private Boolean affectsTimetable = true;
}


@Service
public class AcademicCalendarService {
    
    public boolean isHoliday(LocalDate date) {
        return academicEventRepository.existsByDateAndType(date, EventType.HOLIDAY);
    }
    
    public List<TimetableEntry> getAffectedClasses(LocalDate holidayDate) {
        // Find all classes scheduled on holiday
        return timetableRepository.findByDate(holidayDate);
    }
    
    public void rescheduleClasses(LocalDate fromDate, LocalDate toDate) {
        List<TimetableEntry> affectedClasses = getAffectedClasses(fromDate);
        
        for (TimetableEntry entry : affectedClasses) {
            // Find next available slot
            LocalDate newDate = findNextAvailableDate(toDate, entry);
            createAdjustment(entry, newDate);
        }
    }
}
```

**API Endpoints:**
```
POST   /api/calendar/events                - Add holiday/exam
GET    /api/calendar/events/{year}         - Get academic calendar
POST   /api/calendar/reschedule            - Reschedule classes
GET    /api/calendar/holidays              - Get all holidays
POST   /api/exams/schedule                 - Schedule exam
GET    /api/exams/division/{id}            - Get exam schedule
```

---

### 3. Student Notifications System
**Problem:** Push/email notifications for timetable changes

**Database Schema:**

```sql
-- Notification Settings
CREATE TABLE notification_settings (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    user_type ENUM('student', 'teacher', 'admin') NOT NULL,
    email_enabled TINYINT(1) DEFAULT 1,
    push_enabled TINYINT(1) DEFAULT 1,
    sms_enabled TINYINT(1) DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_user_notification (user_id, user_type)
);


-- Notification Queue
CREATE TABLE notifications (
    id INT PRIMARY KEY AUTO_INCREMENT,
    recipient_id INT NOT NULL,
    recipient_type ENUM('student', 'teacher', 'division', 'all') NOT NULL,
    notification_type ENUM('timetable_change', 'exam_schedule', 'holiday', 'announcement') NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    priority ENUM('low', 'medium', 'high') DEFAULT 'medium',
    status ENUM('pending', 'sent', 'failed') DEFAULT 'pending',
    sent_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_notification_status (status),
    INDEX idx_notification_recipient (recipient_id, recipient_type),
    INDEX idx_notification_created (created_at)
);

-- Notification Delivery Log
CREATE TABLE notification_delivery_log (
    id INT PRIMARY KEY AUTO_INCREMENT,
    notification_id INT NOT NULL,
    delivery_method ENUM('email', 'push', 'sms') NOT NULL,
    status ENUM('success', 'failed') NOT NULL,
    error_message TEXT,
    delivered_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_delivery_notification FOREIGN KEY (notification_id) 
        REFERENCES notifications(id) ON DELETE CASCADE,
    INDEX idx_delivery_status (status)
);
```

**Java Implementation:**

```java
@Service
public class NotificationService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private FirebaseMessaging firebaseMessaging;
    
    public void notifyTimetableChange(TimetableEntry oldEntry, TimetableEntry newEntry) {
        // Get affected students
        List<Student> students = studentRepository.findByDivisionId(oldEntry.getDivision().getId());
        
        String message = buildChangeMessage(oldEntry, newEntry);
        
        for (Student student : students) {
            sendNotification(student, "Timetable Change", message);
        }
    }
