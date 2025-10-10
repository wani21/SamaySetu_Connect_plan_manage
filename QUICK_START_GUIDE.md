# 🚀 Quick Start Guide - SamaySetu Connect Backend

## ⚡ Get Started in 5 Minutes

---

## Step 1: Verify Prerequisites ✅

```bash
# Check Java version (need 17+)
java -version

# Check Maven
mvn -version

# Check MySQL
mysql --version
```

---

## Step 2: Setup Database 🗄️

```sql
-- Login to MySQL
mysql -u root -p

-- Create database
CREATE DATABASE Review1 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Verify
SHOW DATABASES;

-- Exit
exit;
```

---

## Step 3: Configure Application ⚙️

Edit `Backend/src/main/resources/application.properties`:

```properties
spring.application.name=Review1

# Update these if needed
spring.datasource.url=jdbc:mysql://localhost:3306/Review1
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD_HERE

spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
spring.jpa.show-sql=true

server.port=8083
```

---

## Step 4: Run the Application 🏃

### Option A: Using Maven
```bash
cd Backend
./mvnw spring-boot:run
```

### Option B: Using IDE
1. Open `Backend` folder in your IDE
2. Find `Review1Application.java`
3. Right-click → Run

---

## Step 5: Test the API 🧪

### Test 1: Create a Department
```bash
curl -X POST http://localhost:8083/api/departments \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Computer Science",
    "code": "CS",
    "headOfDepartment": "Dr. John Smith"
  }'
```

**Expected Response:**
```
Department added successfully
```

### Test 2: Get All Departments
```bash
curl http://localhost:8083/api/departments
```

**Expected Response:**
```json
[
  {
    "id": 1,
    "name": "Computer Science",
    "code": "CS",
    "headOfDepartment": "Dr. John Smith",
    "createdAt": "2024-10-10T10:30:00",
    "updatedAt": "2024-10-10T10:30:00"
  }
]
```

---

## 📋 Sample Data Setup

### 1. Create Department
```bash
curl -X POST http://localhost:8083/api/departments \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Computer Science",
    "code": "CS",
    "headOfDepartment": "Dr. John Smith"
  }'
```

### 2. Create Academic Year
```bash
curl -X POST http://localhost:8083/api/academic-years \
  -H "Content-Type: application/json" \
  -d '{
    "yearName": "2024-25",
    "startDate": "2024-07-01",
    "endDate": "2025-06-30",
    "isCurrent": true
  }'
```

### 3. Create Teacher
```bash
curl -X POST http://localhost:8083/api/teachers \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Prof. Jane Doe",
    "employeeId": "EMP001",
    "email": "jane.doe@college.edu",
    "phone": "1234567890",
    "weeklyHoursLimit": 25,
    "specialization": "Data Structures, Algorithms",
    "isActive": true,
    "department": {"id": 1}
  }'
```

### 4. Create Course
```bash
curl -X POST http://localhost:8083/api/courses \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Data Structures",
    "code": "CS201",
    "courseType": "THEORY",
    "credits": 4,
    "hoursPerWeek": 4,
    "semester": "SEM_3",
    "description": "Introduction to data structures",
    "isActive": true,
    "department": {"id": 1}
  }'
```

### 5. Create Room
```bash
curl -X POST http://localhost:8083/api/rooms \
  -H "Content-Type: application/json" \
  -d '{
    "name": "CS Lab 1",
    "roomNumber": "CS-101",
    "capacity": 60,
    "roomType": "LAB",
    "hasProjector": true,
    "hasAc": true,
    "equipment": "60 computers, projector",
    "isActive": true,
    "department": {"id": 1}
  }'
```

### 6. Create Division
```bash
curl -X POST http://localhost:8083/api/divisions \
  -H "Content-Type: application/json" \
  -d '{
    "name": "A",
    "year": 2,
    "branch": "Computer Science",
    "totalStudents": 60,
    "isActive": true,
    "department": {"id": 1},
    "academicYear": {"id": 1}
  }'
```

---

## 🔍 Verify Database Tables

```sql
-- Login to MySQL
mysql -u root -p

-- Use database
USE Review1;

-- Show all tables
SHOW TABLES;

-- Expected tables:
-- departments
-- teachers
-- courses
-- rooms
-- divisions
-- academic_years
-- time_slots
-- timetable_entries
-- teacher_availability
-- teacher_courses
-- students

-- Check department data
SELECT * FROM departments;

-- Exit
exit;
```

---

## 🐛 Common Issues & Solutions

### Issue 1: Port Already in Use
```
Error: Port 8083 is already in use
```

**Solution:** Change port in `application.properties`:
```properties
server.port=8084
```

### Issue 2: Database Connection Failed
```
Error: Access denied for user 'root'@'localhost'
```

**Solution:** Update credentials in `application.properties`:
```properties
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD
```

### Issue 3: Table Already Exists Error
```
Error: Table 'departments' already exists
```

**Solution:** Change DDL mode:
```properties
spring.jpa.hibernate.ddl-auto=update
```

### Issue 4: Validation Failed
```
Error: Validation failed for object='departmentEntity'
```

**Solution:** Check that all required fields are provided:
- `name` (required)
- `code` (required)
- `headOfDepartment` (optional)

---

## 📊 Check Application Status

### Health Check
```bash
# Check if application is running
curl http://localhost:8083/api/departments
```

### View Logs
```bash
# In terminal where app is running
# Look for:
# - "Started Review1Application"
# - "Tomcat started on port(s): 8083"
```

### Check Database
```sql
USE Review1;
SHOW TABLES;
SELECT COUNT(*) FROM departments;
```

---

## 🎯 Next Steps

1. ✅ Application running
2. ✅ Database connected
3. ✅ Sample data created
4. ✅ API tested

**Now you can:**
- Create more entities
- Test relationships
- Build the frontend
- Implement timetable logic

---

## 📚 Useful Commands

### Maven Commands
```bash
# Clean and build
./mvnw clean install

# Run tests
./mvnw test

# Package as JAR
./mvnw package
```

### Database Commands
```sql
-- View all departments
SELECT * FROM departments;

-- View all teachers
SELECT * FROM teachers;

-- View relationships
SELECT t.name, d.name 
FROM teachers t 
JOIN departments d ON t.department_id = d.id;

-- Clear all data (careful!)
TRUNCATE TABLE timetable_entries;
TRUNCATE TABLE teacher_courses;
TRUNCATE TABLE teacher_availability;
TRUNCATE TABLE students;
TRUNCATE TABLE divisions;
TRUNCATE TABLE courses;
TRUNCATE TABLE teachers;
TRUNCATE TABLE rooms;
TRUNCATE TABLE time_slots;
TRUNCATE TABLE academic_years;
TRUNCATE TABLE departments;
```

---

## 🎉 Success!

If you've completed all steps, you now have:
- ✅ Running Spring Boot application
- ✅ Connected MySQL database
- ✅ Working REST API
- ✅ Sample data loaded

**You're ready to build the timetable management features!**

---

## 📞 Need Help?

1. Check `IMPLEMENTATION_COMPLETE.md` for detailed info
2. Review `API_ENDPOINTS_REFERENCE.md` for API docs
3. See `ENTITY_IMPLEMENTATION_SUMMARY.md` for entity details
4. Check application logs for errors

---

**Happy Coding! 🚀**
