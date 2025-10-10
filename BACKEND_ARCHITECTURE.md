# Backend Architecture - Spring Boot Implementation

## Technology Stack

- **Framework:** Spring Boot 3.2.x
- **Java Version:** 17 or 21 (LTS)
- **Database:** MySQL 8.0+
- **ORM:** Hibernate (JPA)
- **Build Tool:** Maven 3.9+
- **API Documentation:** Swagger/OpenAPI 3.0
- **Security:** Spring Security 6.x
- **Testing:** JUnit 5, Mockito, TestContainers

---

## Project Structure

```
timetable-backend/
├── src/
│   ├── main/
│   │   ├── java/com/college/timetable/
│   │   │   ├── TimetableApplication.java
│   │   │   ├── config/
│   │   │   ├── entity/
│   │   │   ├── repository/
│   │   │   ├── service/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   ├── exception/
│   │   │   ├── validator/
│   │   │   ├── mapper/
│   │   │   └── util/
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-dev.properties
│   │       ├── application-prod.properties
│   │       └── db/migration/
│   └── test/
│       └── java/com/college/timetable/
├── pom.xml
└── README.md
```

---

## Layer Architecture

### 1. Controller Layer (REST API)
**Responsibility:** Handle HTTP requests, validate input, return responses

### 2. Service Layer
**Responsibility:** Business logic, transaction management, validation

### 3. Repository Layer
**Responsibility:** Database operations, queries

### 4. Entity Layer
**Responsibility:** JPA entities mapping to database tables

### 5. DTO Layer
**Responsibility:** Data transfer between layers

---

## Configuration Files

### pom.xml Dependencies

```xml
<dependencies>
    <!-- Spring Boot Starters -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    
    <!-- Database -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
    </dependency>
    
    <!-- Utilities -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
    </dependency>
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
        <version>1.5.5.Final</version>
    </dependency>
    
    <!-- API Documentation -->
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>2.3.0</version>
    </dependency>
    
    <!-- Testing -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### application.properties
```properties
# Application
spring.application.name=college-timetable-management
server.port=8080

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/college_timetable
spring.datasource.username=root
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.use_sql_comments=true

# Connection Pool
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000

# Logging
logging.level.root=INFO
logging.level.com.college.timetable=DEBUG
logging.level.org.hibernate.SQL=DEBUG

# API Documentation
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
```

---

## Entity Layer Implementation

### Base Entity
```java
@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

### Teacher Entity
```java
@Entity
@Table(name = "teachers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Teacher extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @NotNull
    @Size(max = 100)
    private String name;
    
    @NotNull
    @Column(name = "employee_id", unique = true)
    private String employeeId;
    
    @Email
    @Column(unique = true)
    private String email;
    
    @Size(max = 15)
    private String phone;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;
    
    @Column(name = "weekly_hours_limit")
    private Integer weeklyHoursLimit = 25;
    
    @Column(columnDefinition = "TEXT")
    private String specialization;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @OneToMany(mappedBy = "teacher", cascade = CascadeType.ALL)
    private List<TeacherAvailability> availabilities;
    
    @ManyToMany
    @JoinTable(
        name = "teacher_courses",
        joinColumns = @JoinColumn(name = "teacher_id"),
        inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    private Set<Course> courses;
}
```

### TimetableEntry Entity
```java
@Entity
@Table(name = "timetable_entries")
@Getter
@Setter
@NoArgsConstructor
public class TimetableEntry extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "division_id", nullable = false)
    private Division division;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_slot_id", nullable = false)
    private TimeSlot timeSlot;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;
    
    @Column(name = "week_number")
    private Integer weekNumber = 1;
    
    @Column(name = "is_recurring")
    private Boolean isRecurring = true;
    
    @Column(columnDefinition = "TEXT")
    private String notes;
}
```

---

## Repository Layer

### TimetableEntryRepository
```java
@Repository
public interface TimetableEntryRepository extends JpaRepository<TimetableEntry, Integer> {
    
    // Find by division
    List<TimetableEntry> findByDivisionIdAndAcademicYearId(
        Integer divisionId, Integer academicYearId
    );
    
    // Find by teacher
    List<TimetableEntry> findByTeacherIdAndAcademicYearId(
        Integer teacherId, Integer academicYearId
    );
    
    // Check teacher conflict
    @Query("SELECT COUNT(te) FROM TimetableEntry te WHERE " +
           "te.teacher.id = :teacherId AND " +
           "te.dayOfWeek = :dayOfWeek AND " +
           "te.timeSlot.id = :timeSlotId AND " +
           "te.academicYear.id = :academicYearId AND " +
           "(:entryId IS NULL OR te.id != :entryId)")
    long countTeacherConflicts(
        @Param("teacherId") Integer teacherId,
        @Param("dayOfWeek") DayOfWeek dayOfWeek,
        @Param("timeSlotId") Integer timeSlotId,
        @Param("academicYearId") Integer academicYearId,
        @Param("entryId") Integer entryId
    );
    
    // Check room conflict
    @Query("SELECT COUNT(te) FROM TimetableEntry te WHERE " +
           "te.room.id = :roomId AND " +
           "te.dayOfWeek = :dayOfWeek AND " +
           "te.timeSlot.id = :timeSlotId AND " +
           "te.academicYear.id = :academicYearId AND " +
           "(:entryId IS NULL OR te.id != :entryId)")
    long countRoomConflicts(
        @Param("roomId") Integer roomId,
        @Param("dayOfWeek") DayOfWeek dayOfWeek,
        @Param("timeSlotId") Integer timeSlotId,
        @Param("academicYearId") Integer academicYearId,
        @Param("entryId") Integer entryId
    );
    
    // Calculate teacher weekly hours
    @Query("SELECT COALESCE(SUM(ts.durationMinutes), 0) FROM TimetableEntry te " +
           "JOIN te.timeSlot ts WHERE " +
           "te.teacher.id = :teacherId AND " +
           "te.academicYear.id = :academicYearId AND " +
           "(:entryId IS NULL OR te.id != :entryId)")
    Integer calculateTeacherWeeklyMinutes(
        @Param("teacherId") Integer teacherId,
        @Param("academicYearId") Integer academicYearId,
        @Param("entryId") Integer entryId
    );
}
```

---

## Service Layer

### ConflictValidationService
