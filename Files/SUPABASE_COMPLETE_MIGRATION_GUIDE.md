# Complete Supabase Migration Guide

## Overview
This guide provides step-by-step instructions to migrate your SamaySetu application from MySQL to Supabase (PostgreSQL) with a clean, properly formatted database schema.

## What's Changed

### Table Renames (Clean Schema)
- `teachers` → `users` (more generic for all user types)
- `rooms` → `classrooms` (clearer naming)
- `teacher_availability` → `user_availability` (matches new users table)
- `teacher_courses` → Still uses `user_id` column (updated foreign key)

### Data Type Conversions
- `BIT(1)` → `BOOLEAN`
- `datetime(6)` → `TIMESTAMP`
- `AUTO_INCREMENT` → `BIGSERIAL`
- `tinyint(1)` → `BOOLEAN`
- `ENUM` → `VARCHAR` with `CHECK` constraints

### Removed Tables
- `acadamic_year` (old unused table)
- `acadamic_year_seq` (old sequence table)

## Migration Steps

### Step 1: Create Supabase Project

1. Go to [https://supabase.com](https://supabase.com)
2. Sign up or log in
3. Click "New Project"
4. Fill in project details:
   - **Name**: samaysetu
   - **Database Password**: Choose a strong password (save it!)
   - **Region**: Choose closest to your location
   - **Pricing Plan**: Free tier is sufficient
5. Wait for project to be created (2-3 minutes)

### Step 2: Run Migration Script

1. In your Supabase project dashboard, go to **SQL Editor**
2. Click **New Query**
3. Open `Scripts/supabase_complete_migration.sql` from your project
4. Copy the entire script
5. Paste it into the SQL Editor
6. Click **Run** or press `Ctrl+Enter`
7. Wait for execution to complete
8. Check the verification results at the bottom

**Expected Results:**
```
academic_years: 3 records
departments: 5 records
users: 3 records
divisions: 5 records
batches: 6 records
courses: 1 record
classrooms: 4 records
time_slots: 22 records
students: 0 records
user_availability: 0 records
teacher_courses: 0 records
timetable_entries: 0 records
```

### Step 3: Get Supabase Connection Details

1. In Supabase dashboard, go to **Settings** → **Database**
2. Scroll to **Connection String** section
3. Select **Java** tab
4. Copy the connection details:
   - **Host**: `db.xxxxxxxxxxxxx.supabase.co`
   - **Port**: `5432`
   - **Database**: `postgres`
   - **User**: `postgres`
   - **Password**: Your database password

### Step 4: Update Spring Boot Configuration

#### 4.1 Update `pom.xml`

Replace MySQL driver with PostgreSQL driver:

```xml
<!-- Remove MySQL dependency -->
<!-- <dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency> -->

<!-- Add PostgreSQL dependency -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

#### 4.2 Update `application.properties`

Replace MySQL configuration with Supabase PostgreSQL:

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

# Connection Pool Settings
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
```

**Important:** Replace `xxxxxxxxxxxxx` with your actual Supabase project reference and `YOUR_SUPABASE_PASSWORD` with your database password.

### Step 5: Update Entity Classes

You need to update the `@Table` annotations in your entity classes to match the new table names:

#### 5.1 Update `TeacherEntity.java`

```java
@Entity
@Table(name = "users")  // Changed from "teachers"
public class TeacherEntity {
    // Rest of the code remains the same
}
```

#### 5.2 Update `ClassRoom.java`

```java
@Entity
@Table(name = "classrooms")  // Changed from "rooms"
public class ClassRoom {
    // Rest of the code remains the same
}
```

#### 5.3 Update `TeacherAvailability.java`

```java
@Entity
@Table(name = "user_availability")  // Changed from "teacher_availability"
public class TeacherAvailability {
    // Update foreign key reference
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)  // Changed from "teacher_id"
    private TeacherEntity teacher;
}
```

#### 5.4 Update Junction Table (if you have a separate entity)

If you have a `TeacherCourses` entity:

```java
@Entity
@Table(name = "teacher_courses")
public class TeacherCourses {
    @ManyToOne
    @JoinColumn(name = "user_id")  // Changed from "teacher_id"
    private TeacherEntity teacher;
    
    @ManyToOne
    @JoinColumn(name = "course_id")
    private CourseEntity course;
}
```

#### 5.5 Update `TimetableEntry.java`

```java
@Entity
@Table(name = "timetable_entries")
public class TimetableEntry {
    // Update foreign key references
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)  // Changed from "teacher_id"
    private TeacherEntity teacher;
    
    @ManyToOne
    @JoinColumn(name = "classroom_id", nullable = false)  // Changed from "room_id"
    private ClassRoom classroom;
}
```

### Step 6: Update Repository Queries

If you have custom queries in your repositories that reference old table names, update them:

#### Example: `Teacher_Repo.java`

```java
// If you have native queries, update table names
@Query(value = "SELECT * FROM users WHERE role = 'TEACHER'", nativeQuery = true)
List<TeacherEntity> findAllTeachers();
```

### Step 7: Test the Migration

1. **Clean and rebuild** your project:
   ```bash
   mvn clean install
   ```

2. **Start the application**:
   ```bash
   mvn spring-boot:run
   ```

3. **Check logs** for any errors related to database connection or table names

4. **Test key endpoints**:
   - Login: `POST /api/auth/login`
   - Get departments: `GET /api/departments`
   - Get academic years: `GET /api/academic-years`
   - Get users: `GET /api/admin/staff`

5. **Verify data** in Supabase:
   - Go to **Table Editor** in Supabase dashboard
   - Check each table to ensure data is present
   - Verify relationships are working

### Step 8: Update Frontend (if needed)

The frontend should work without changes since the API endpoints remain the same. However, verify:

1. All API calls are working
2. Data is being displayed correctly
3. CRUD operations are functioning

## Troubleshooting

### Connection Issues

**Error**: `Connection refused`
- Check if your IP is allowed in Supabase
- Go to **Settings** → **Database** → **Connection Pooling**
- Enable connection pooling if needed

**Error**: `Authentication failed`
- Verify your database password
- Check username is `postgres`
- Ensure connection string is correct

### Entity Mapping Issues

**Error**: `Table "teachers" doesn't exist`
- You forgot to update `@Table(name = "users")` in `TeacherEntity.java`
- Rebuild the project after making changes

**Error**: `Column "teacher_id" doesn't exist`
- Update foreign key column names in entity classes
- Change `teacher_id` to `user_id`
- Change `room_id` to `classroom_id`

### Data Type Issues

**Error**: `column "is_active" is of type boolean but expression is of type bit`
- This shouldn't happen with the migration script
- If it does, check that the migration script ran completely

## Rollback Plan

If you need to rollback to MySQL:

1. Keep your MySQL backup: `mysql_backups/samaysetu_backup.sql`
2. Restore MySQL database:
   ```bash
   mysql -u root -p samaysetu < mysql_backups/samaysetu_backup.sql
   ```
3. Revert `application.properties` to MySQL configuration
4. Revert `pom.xml` to use MySQL driver
5. Revert entity `@Table` annotations to old names

## Benefits of Supabase

1. **Free Tier**: 500MB database, 2GB bandwidth
2. **Auto Backups**: Daily backups included
3. **Real-time**: Built-in real-time subscriptions
4. **REST API**: Auto-generated REST API
5. **Dashboard**: Beautiful UI for database management
6. **Security**: Row Level Security (RLS) support
7. **Scalability**: Easy to upgrade as you grow

## Next Steps

After successful migration:

1. **Enable Row Level Security (RLS)** in Supabase for better security
2. **Set up automated backups** (already included in free tier)
3. **Monitor performance** using Supabase dashboard
4. **Consider using Supabase Auth** for authentication (optional)
5. **Update documentation** with new connection details

## Support

If you encounter issues:

1. Check Supabase logs in dashboard
2. Check Spring Boot application logs
3. Verify all entity classes are updated
4. Ensure PostgreSQL driver is in classpath
5. Test connection using a PostgreSQL client (DBeaver, pgAdmin)

## Summary of Files to Update

### Backend Files:
1. `pom.xml` - Add PostgreSQL driver
2. `application.properties` - Update connection details
3. `TeacherEntity.java` - Change table name to "users"
4. `ClassRoom.java` - Change table name to "classrooms"
5. `TeacherAvailability.java` - Change table name and foreign key
6. `TimetableEntry.java` - Update foreign key column names
7. Any repository with native queries

### No Changes Needed:
- Frontend code
- API endpoints
- Business logic
- Service classes (unless they have native queries)

## Migration Checklist

- [ ] Create Supabase project
- [ ] Run migration script in SQL Editor
- [ ] Verify data migration (check record counts)
- [ ] Update `pom.xml` with PostgreSQL driver
- [ ] Update `application.properties` with Supabase connection
- [ ] Update `TeacherEntity.java` table name
- [ ] Update `ClassRoom.java` table name
- [ ] Update `TeacherAvailability.java` table name and foreign key
- [ ] Update `TimetableEntry.java` foreign key columns
- [ ] Update any custom native queries
- [ ] Clean and rebuild project
- [ ] Test application startup
- [ ] Test login functionality
- [ ] Test CRUD operations
- [ ] Verify data in Supabase dashboard
- [ ] Update deployment documentation
- [ ] Backup old MySQL database (already done)

---

**Migration Date**: 2026-01-31  
**Database**: samaysetu  
**Source**: MySQL 8.0.42  
**Target**: PostgreSQL (Supabase)  
**Status**: Ready for execution
