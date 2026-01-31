# Entity Update Checklist for Supabase Migration

## Quick Reference: What Needs to Change

### Table Name Changes
| Old Table Name | New Table Name | Entity File |
|----------------|----------------|-------------|
| `teachers` | `users` | `TeacherEntity.java` |
| `rooms` | `classrooms` | `ClassRoom.java` |
| `teacher_availability` | `user_availability` | `TeacherAvailability.java` |

### Foreign Key Column Changes
| Old Column Name | New Column Name | Affected Entities |
|-----------------|-----------------|-------------------|
| `teacher_id` | `user_id` | `TeacherAvailability.java`, `TimetableEntry.java`, Junction table |
| `room_id` | `classroom_id` | `TimetableEntry.java` |

## Detailed Changes Required

### 1. TeacherEntity.java
**Location**: `Backend/src/main/java/com/College/timetable/Entity/TeacherEntity.java`

**Change**:
```java
@Entity
@Table(name = "users")  // Changed from "teachers"
public class TeacherEntity {
    // No other changes needed
}
```

---

### 2. ClassRoom.java
**Location**: `Backend/src/main/java/com/College/timetable/Entity/ClassRoom.java`

**Change**:
```java
@Entity
@Table(name = "classrooms")  // Changed from "rooms"
public class ClassRoom {
    // No other changes needed
}
```

---

### 3. TeacherAvailability.java
**Location**: `Backend/src/main/java/com/College/timetable/Entity/TeacherAvailability.java`

**Changes**:
```java
@Entity
@Table(name = "user_availability")  // Changed from "teacher_availability"
public class TeacherAvailability {
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)  // Changed from "teacher_id"
    private TeacherEntity teacher;
    
    // Rest remains the same
}
```

---

### 4. TimetableEntry.java
**Location**: `Backend/src/main/java/com/College/timetable/Entity/TimetableEntry.java`

**Changes**:
```java
@Entity
@Table(name = "timetable_entries")
public class TimetableEntry {
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)  // Changed from "teacher_id"
    private TeacherEntity teacher;
    
    @ManyToOne
    @JoinColumn(name = "classroom_id", nullable = false)  // Changed from "room_id"
    private ClassRoom classroom;
    
    // Rest remains the same
}
```

---

### 5. TeacherCred.java (if it exists as separate entity)
**Location**: `Backend/src/main/java/com/College/timetable/Entity/TeacherCred.java`

**Check if this is a junction table entity**. If yes:
```java
@Entity
@Table(name = "teacher_courses")
public class TeacherCred {
    
    @ManyToOne
    @JoinColumn(name = "user_id")  // Changed from "teacher_id"
    private TeacherEntity teacher;
    
    @ManyToOne
    @JoinColumn(name = "course_id")
    private CourseEntity course;
}
```

---

## Repository Updates

### Check These Repository Files for Native Queries

1. **Teacher_Repo.java**
   - Location: `Backend/src/main/java/com/College/timetable/Repository/Teacher_Repo.java`
   - Look for: `@Query` annotations with `nativeQuery = true`
   - Update: Change `teachers` to `users` in SQL queries

2. **Room_repo.java**
   - Location: `Backend/src/main/java/com/College/timetable/Repository/Room_repo.java`
   - Look for: `@Query` annotations with `nativeQuery = true`
   - Update: Change `rooms` to `classrooms` in SQL queries

3. **TeacherAvailability_repo.java**
   - Location: `Backend/src/main/java/com/College/timetable/Repository/TeacherAvailability_repo.java`
   - Look for: `@Query` annotations with `nativeQuery = true`
   - Update: Change `teacher_availability` to `user_availability` in SQL queries
   - Update: Change `teacher_id` to `user_id` in SQL queries

4. **TimetableEntry_repo.java**
   - Location: `Backend/src/main/java/com/College/timetable/Repository/TimetableEntry_repo.java`
   - Look for: `@Query` annotations with `nativeQuery = true`
   - Update: Change `teacher_id` to `user_id` in SQL queries
   - Update: Change `room_id` to `classroom_id` in SQL queries

---

## Service Layer Updates

### Check These Service Files

Most service files won't need changes unless they contain:
- Native SQL queries
- Direct JDBC calls
- Table name references in strings

**Files to check**:
1. `TeacherService.java`
2. `RoomService.java`
3. `TimetableService.java`
4. `TableService.java`

---

## Configuration Updates

### 1. pom.xml
**Location**: `Backend/pom.xml`

**Remove**:
```xml
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

**Add**:
```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

### 2. application.properties
**Location**: `Backend/src/main/resources/application.properties`

**Replace MySQL config with**:
```properties
# Database Configuration - Supabase PostgreSQL
spring.datasource.url=jdbc:postgresql://db.xxxxxxxxxxxxx.supabase.co:5432/postgres
spring.datasource.username=postgres
spring.datasource.password=YOUR_SUPABASE_PASSWORD
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA Configuration for PostgreSQL
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=none
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

---

## Testing Checklist

After making all changes:

### Build & Compile
- [ ] Run `mvn clean install`
- [ ] Check for compilation errors
- [ ] Verify no missing dependencies

### Application Startup
- [ ] Run `mvn spring-boot:run`
- [ ] Check logs for database connection success
- [ ] Verify no table/column not found errors

### API Testing
- [ ] Test login: `POST /api/auth/login`
- [ ] Test get users: `GET /api/admin/staff`
- [ ] Test get departments: `GET /api/departments`
- [ ] Test get classrooms: `GET /api/rooms`
- [ ] Test get time slots: `GET /api/timeslots`
- [ ] Test create division: `POST /api/divisions`
- [ ] Test create batch: `POST /api/batches`

### Data Verification
- [ ] Login with existing admin account
- [ ] Verify all departments are visible
- [ ] Verify all divisions are visible
- [ ] Verify all batches are visible
- [ ] Verify all classrooms are visible
- [ ] Verify all time slots are visible

---

## Common Errors & Solutions

### Error: Table "teachers" doesn't exist
**Solution**: Update `@Table(name = "users")` in `TeacherEntity.java`

### Error: Column "teacher_id" doesn't exist
**Solution**: Update `@JoinColumn(name = "user_id")` in affected entities

### Error: Table "rooms" doesn't exist
**Solution**: Update `@Table(name = "classrooms")` in `ClassRoom.java`

### Error: Column "room_id" doesn't exist
**Solution**: Update `@JoinColumn(name = "classroom_id")` in `TimetableEntry.java`

### Error: Driver class not found
**Solution**: Ensure PostgreSQL driver is in `pom.xml` and run `mvn clean install`

### Error: Connection refused
**Solution**: Check Supabase connection string and password in `application.properties`

---

## Quick Command Reference

```bash
# Clean and rebuild project
mvn clean install

# Run application
mvn spring-boot:run

# Run with debug logs
mvn spring-boot:run -Dspring-boot.run.arguments=--logging.level.org.hibernate.SQL=DEBUG

# Skip tests during build
mvn clean install -DskipTests
```

---

## Files Summary

### Must Update (5 files):
1. `pom.xml`
2. `application.properties`
3. `TeacherEntity.java`
4. `ClassRoom.java`
5. `TeacherAvailability.java`

### Check and Update if Needed (4 files):
1. `TimetableEntry.java`
2. `Teacher_Repo.java`
3. `Room_repo.java`
4. `TeacherAvailability_repo.java`

### No Changes Needed:
- All other entity files
- Frontend files
- Controller files (unless they have native queries)
- Most service files

---

**Total Estimated Time**: 15-30 minutes  
**Difficulty**: Easy to Medium  
**Risk Level**: Low (we have MySQL backup for rollback)
