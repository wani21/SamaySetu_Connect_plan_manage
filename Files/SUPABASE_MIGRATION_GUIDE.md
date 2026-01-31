# 🚀 Supabase Migration Guide

Complete guide to migrate SamaySetu from MySQL to Supabase PostgreSQL with clean, properly formatted schema.

## 📋 Overview

This guide will help you:
1. Export data from MySQL
2. Create clean, properly named tables in Supabase
3. Migrate data without loss
4. Update Spring Boot configuration
5. Test the migration

## 🎯 Table Naming Improvements

### Current (MySQL) → New (Supabase)
```
teachers              → users (more generic, includes all roles)
academic_years        → academic_years (keep as is)
departments           → departments (keep as is)
courses               → courses (keep as is)
divisions             → divisions (keep as is)
batches               → batches (keep as is)
time_slots            → time_slots (keep as is)
class_rooms           → classrooms (remove underscore)
timetable_entries     → timetable_entries (keep as is)
teacher_availability  → user_availability (align with users table)
```

## 📝 Step 1: Setup Supabase Account

### 1.1 Create Supabase Project
```bash
1. Go to https://supabase.com
2. Sign up / Login
3. Click "New Project"
4. Fill in details:
   - Name: SamaySetu
   - Database Password: [Strong Password]
   - Region: [Closest to you]
   - Pricing Plan: Free
5. Wait for project to be ready (2-3 minutes)
```

### 1.2 Get Connection Details
```bash
1. Go to Project Settings → Database
2. Note down:
   - Host: db.[project-ref].supabase.co
   - Database name: postgres
   - Port: 5432
   - User: postgres
   - Password: [Your password]
   - Connection String (for Spring Boot)
```

## 📊 Step 2: Export Current MySQL Data

### 2.1 Export Schema and Data
```bash
# Export all data
mysqldump -u samaysetu_user -p samaysetu_db > mysql_backup.sql

# Export only data (no CREATE statements)
mysqldump -u samaysetu_user -p --no-create-info samaysetu_db > mysql_data_only.sql

# Export specific tables
mysqldump -u samaysetu_user -p samaysetu_db teachers academic_years departments courses divisions batches time_slots class_rooms > mysql_export.sql
```

### 2.2 Export as CSV (Recommended for migration)
```sql
-- Connect to MySQL
mysql -u samaysetu_user -p samaysetu_db

-- Export each table as CSV
SELECT * FROM teachers 
INTO OUTFILE '/tmp/teachers.csv' 
FIELDS TERMINATED BY ',' 
ENCLOSED BY '"' 
LINES TERMINATED BY '\n';

SELECT * FROM academic_years 
INTO OUTFILE '/tmp/academic_years.csv' 
FIELDS TERMINATED BY ',' 
ENCLOSED BY '"' 
LINES TERMINATED BY '\n';

-- Repeat for all tables
```

## 🗄️ Step 3: Create Clean Schema in Supabase

### 3.1 Connect to Supabase
```bash
# Using psql
psql "postgresql://postgres:[PASSWORD]@db.[PROJECT-REF].supabase.co:5432/postgres"

# Or use Supabase SQL Editor in dashboard
```

### 3.2 Run the Clean Schema Script
Use the provided `Scripts/supabase_clean_schema.sql` script (created below)

## 🔄 Step 4: Migrate Data

### 4.1 Using Supabase Dashboard
```bash
1. Go to Table Editor in Supabase Dashboard
2. Select table
3. Click "Insert" → "Import data from CSV"
4. Upload CSV file
5. Map columns
6. Import
```

### 4.2 Using SQL (Recommended)
Use the provided `Scripts/supabase_data_migration.sql` script

## ⚙️ Step 5: Update Spring Boot Configuration

### 5.1 Update pom.xml
Add PostgreSQL driver, remove MySQL driver:
```xml
<!-- Remove MySQL Driver -->
<!-- <dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency> -->

<!-- Add PostgreSQL Driver -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

### 5.2 Update application.properties
```properties
# Supabase PostgreSQL Configuration
spring.datasource.url=jdbc:postgresql://db.[PROJECT-REF].supabase.co:5432/postgres
spring.datasource.username=postgres
spring.datasource.password=[YOUR-SUPABASE-PASSWORD]
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA/Hibernate Configuration for PostgreSQL
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.jdbc.lob.non_contextual_creation=true

# Connection Pool (HikariCP)
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000

# SSL Configuration (Supabase requires SSL)
spring.datasource.hikari.data-source-properties.ssl=true
spring.datasource.hikari.data-source-properties.sslmode=require
```

## 🔧 Step 6: Update Entity Classes

### 6.1 Update Table Names in Entities
Update `@Table` annotations to use new names:

```java
// TeacherEntity.java → UserEntity.java (rename file)
@Entity
@Table(name = "users")  // Changed from "teachers"
public class UserEntity {
    // ... rest of code
}

// ClassRoom.java
@Entity
@Table(name = "classrooms")  // Changed from "class_rooms"
public class ClassRoom {
    // ... rest of code
}
```

### 6.2 Update Sequence Generators
PostgreSQL uses sequences differently:
```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

## 🧪 Step 7: Test Migration

### 7.1 Test Database Connection
```bash
# Build and run
mvn clean compile
mvn spring-boot:run

# Check logs for successful connection
```

### 7.2 Test API Endpoints
```bash
# Test login
curl -X POST http://localhost:8083/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@mitaoe.ac.in","password":"admin123"}'

# Test data retrieval
curl -X GET http://localhost:8083/admin/api/academic-years \
  -H "Authorization: Bearer [TOKEN]"
```

### 7.3 Verify Data Integrity
```sql
-- Connect to Supabase
-- Check record counts
SELECT 'users' as table_name, COUNT(*) as count FROM users
UNION ALL
SELECT 'academic_years', COUNT(*) FROM academic_years
UNION ALL
SELECT 'departments', COUNT(*) FROM departments
UNION ALL
SELECT 'courses', COUNT(*) FROM courses
UNION ALL
SELECT 'divisions', COUNT(*) FROM divisions
UNION ALL
SELECT 'batches', COUNT(*) FROM batches
UNION ALL
SELECT 'time_slots', COUNT(*) FROM time_slots
UNION ALL
SELECT 'classrooms', COUNT(*) FROM classrooms;
```

## 🔒 Step 8: Supabase Security Setup

### 8.1 Enable Row Level Security (RLS)
```sql
-- Enable RLS on all tables
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE academic_years ENABLE ROW LEVEL SECURITY;
ALTER TABLE departments ENABLE ROW LEVEL SECURITY;
ALTER TABLE courses ENABLE ROW LEVEL SECURITY;
ALTER TABLE divisions ENABLE ROW LEVEL SECURITY;
ALTER TABLE batches ENABLE ROW LEVEL SECURITY;
ALTER TABLE time_slots ENABLE ROW LEVEL SECURITY;
ALTER TABLE classrooms ENABLE ROW LEVEL SECURITY;

-- Create policies (example for users table)
CREATE POLICY "Users can view their own data"
ON users FOR SELECT
USING (auth.uid()::text = id::text);

CREATE POLICY "Admins can view all users"
ON users FOR SELECT
USING (
  EXISTS (
    SELECT 1 FROM users
    WHERE id = auth.uid()::text
    AND role = 'ADMIN'
  )
);
```

### 8.2 Create Database Functions (Optional)
```sql
-- Function to get current academic year
CREATE OR REPLACE FUNCTION get_current_academic_year()
RETURNS TABLE (
  id BIGINT,
  year_name VARCHAR,
  start_date DATE,
  end_date DATE
) AS $$
BEGIN
  RETURN QUERY
  SELECT ay.id, ay.year_name, ay.start_date, ay.end_date
  FROM academic_years ay
  WHERE ay.is_current = true
  LIMIT 1;
END;
$$ LANGUAGE plpgsql;
```

## 📊 Step 9: Performance Optimization

### 9.1 Create Indexes
```sql
-- Users table indexes
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_employee_id ON users(employee_id);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_department ON users(department_id);

-- Academic structure indexes
CREATE INDEX idx_departments_academic_year ON departments(academic_year_id);
CREATE INDEX idx_divisions_academic_year ON divisions(academic_year_id);
CREATE INDEX idx_divisions_department ON divisions(department_id);
CREATE INDEX idx_courses_department ON courses(department_id);
CREATE INDEX idx_batches_division ON batches(division_id);

-- Time slots indexes
CREATE INDEX idx_time_slots_type ON time_slots(type);
CREATE INDEX idx_time_slots_active ON time_slots(is_active);
```

### 9.2 Analyze Tables
```sql
-- Update statistics for query optimizer
ANALYZE users;
ANALYZE academic_years;
ANALYZE departments;
ANALYZE courses;
ANALYZE divisions;
ANALYZE batches;
ANALYZE time_slots;
ANALYZE classrooms;
```

## 🔄 Step 10: Backup Strategy

### 10.1 Supabase Automatic Backups
- Free tier: Daily backups (7 days retention)
- Pro tier: Point-in-time recovery

### 10.2 Manual Backup Script
```bash
#!/bin/bash
# backup_supabase.sh

PROJECT_REF="your-project-ref"
PASSWORD="your-password"
BACKUP_DIR="/path/to/backups"
DATE=$(date +%Y%m%d_%H%M%S)

# Create backup
pg_dump "postgresql://postgres:${PASSWORD}@db.${PROJECT_REF}.supabase.co:5432/postgres" \
  > "${BACKUP_DIR}/supabase_backup_${DATE}.sql"

# Compress
gzip "${BACKUP_DIR}/supabase_backup_${DATE}.sql"

echo "Backup completed: supabase_backup_${DATE}.sql.gz"
```

## 🚨 Troubleshooting

### Issue: Connection Timeout
```properties
# Increase timeout in application.properties
spring.datasource.hikari.connection-timeout=60000
```

### Issue: SSL Connection Error
```properties
# Add SSL parameters
spring.datasource.url=jdbc:postgresql://db.[PROJECT-REF].supabase.co:5432/postgres?sslmode=require
```

### Issue: Sequence Issues
```sql
-- Reset sequences if needed
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
SELECT setval('academic_years_id_seq', (SELECT MAX(id) FROM academic_years));
-- Repeat for all tables
```

### Issue: Data Type Mismatches
```sql
-- Check column types
SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'users';
```

## ✅ Migration Checklist

### Pre-Migration:
- [ ] Supabase project created
- [ ] Connection details noted
- [ ] MySQL data exported (SQL and CSV)
- [ ] Backup of current database created

### Migration:
- [ ] Clean schema created in Supabase
- [ ] Data imported successfully
- [ ] Record counts verified
- [ ] Foreign keys working
- [ ] Indexes created

### Post-Migration:
- [ ] Spring Boot configuration updated
- [ ] PostgreSQL driver added to pom.xml
- [ ] Entity classes updated
- [ ] Application builds successfully
- [ ] Application connects to Supabase
- [ ] All API endpoints tested
- [ ] Authentication working
- [ ] CRUD operations working
- [ ] File uploads working

### Optimization:
- [ ] Indexes created
- [ ] RLS policies configured
- [ ] Performance tested
- [ ] Backup strategy implemented

## 📞 Support

### Supabase Resources:
- Documentation: https://supabase.com/docs
- Dashboard: https://app.supabase.com
- Community: https://github.com/supabase/supabase/discussions

### Common Issues:
- Check Supabase status: https://status.supabase.com
- Review connection logs in Supabase Dashboard
- Check Spring Boot logs for detailed errors

---

**Migration Guide for MIT Academy of Engineering**

© 2024 MIT Academy of Engineering - SamaySetu Development Team