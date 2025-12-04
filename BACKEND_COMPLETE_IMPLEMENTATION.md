# Backend Complete Implementation - Phase 1

## Overview
Complete backend REST API implementation for SamaySetu Timetable Management System with full CRUD operations for all entities.

## Completed Features

### 1. Department Management (✅ Complete)
**Endpoint:** `/admin/api/departments`

**Operations:**
- `POST /admin/api/departments` - Create new department
- `GET /admin/api/departments` - Get all departments
- `GET /admin/api/departments/{id}` - Get department by ID
- `PUT /admin/api/departments/{id}` - Update department
- `DELETE /admin/api/departments/{id}` - Delete department

**Fields:**
- name (required, unique)
- code (required, unique)
- headOfDepartment (optional)

---

### 2. Teacher Management (✅ Complete)
**Endpoint:** `/api/teachers`

**Operations:**
- `POST /api/teachers` - Create new teacher
- `GET /api/teachers` - Get all teachers
- `GET /api/teachers/{id}` - Get teacher by ID
- `GET /api/teachers/profile` - Get current logged-in teacher profile
- `PUT /api/teachers/{id}` - Update teacher
- `DELETE /api/teachers/{id}` - Delete teacher

**Fields:**
- name (required)
- employeeId (required, unique)
- email (required, unique)
- phone (optional)
- weeklyHoursLimit (default: 25)
- specialization (optional)
- isActive (default: true)
- department (foreign key)

**Authentication Features:**
- Email verification on registration
- Password reset functionality
- JWT-based authentication
- Role-based access control (TEACHER, ADMIN)

---

### 3. Course Management (✅ Complete)
**Endpoint:** `/admin/api/courses`

**Operations:**
- `POST /admin/api/courses` - Create new course
- `GET /admin/api/courses` - Get all courses
- `GET /admin/api/courses/{id}` - Get course by ID
- `PUT /admin/api/courses/{id}` - Update course
- `DELETE /admin/api/courses/{id}` - Delete course

**Fields:**
- name (required)
- code (required, unique)
- courseType (THEORY, LAB, PRACTICAL, ELECTIVE)
- credits (required, min: 1)
- hoursPerWeek (required, min: 1)
- semester (FIRST, SECOND, THIRD, FOURTH, FIFTH, SIXTH, SEVENTH, EIGHTH)
- description (optional)
- prerequisites (optional)
- isActive (default: true)
- department (foreign key, required)

---

### 4. Room Management (✅ Complete)
**Endpoint:** `/admin/api/rooms`

**Operations:**
- `POST /admin/api/rooms` - Create new room
- `GET /admin/api/rooms` - Get all rooms
- `GET /admin/api/rooms/{id}` - Get room by ID
- `PUT /admin/api/rooms/{id}` - Update room
- `DELETE /admin/api/rooms/{id}` - Delete room

**Fields:**
- name (required, unique)
- roomNumber (required, unique)
- capacity (required, min: 1)
- roomType (CLASSROOM, LAB, AUDITORIUM, SEMINAR_HALL, CONFERENCE_ROOM)
- hasProjector (default: false)
- hasAc (default: false)
- equipment (optional)
- isActive (default: true)
- department (foreign key, optional)

---

### 5. Academic Year Management (✅ Complete)
**Endpoint:** `/admin/api/academic-years`

**Operations:**
- `POST /admin/api/academic-years` - Create new academic year
- `GET /admin/api/academic-years` - Get all academic years
- `GET /admin/api/academic-years/{id}` - Get academic year by ID
- `PUT /admin/api/academic-years/{id}` - Update academic year
- `DELETE /admin/api/academic-years/{id}` - Delete academic year

**Fields:**
- yearName (required, unique) - e.g., "2024-2025"
- startDate (required)
- endDate (required)
- isCurrent (default: false)

---

### 6. Division Management (✅ Complete)
**Endpoint:** `/admin/api/divisions`

**Operations:**
- `POST /admin/api/divisions` - Create new division
- `GET /admin/api/divisions` - Get all divisions
- `GET /admin/api/divisions/{id}` - Get division by ID
- `PUT /admin/api/divisions/{id}` - Update division
- `DELETE /admin/api/divisions/{id}` - Delete division

**Fields:**
- name (required) - e.g., "A", "B", "C"
- year (required, 1-4) - 1=FE, 2=SE, 3=TE, 4=BE
- branch (required) - e.g., "Computer Engineering"
- totalStudents (default: 0)
- isActive (default: true)
- department (foreign key, required)
- academicYear (foreign key, required)

---

### 7. Time Slot Management (✅ Complete - NEW)
**Endpoint:** `/admin/api/time-slots`

**Operations:**
- `POST /admin/api/time-slots` - Create new time slot
- `GET /admin/api/time-slots` - Get all time slots
- `GET /admin/api/time-slots/{id}` - Get time slot by ID
- `PUT /admin/api/time-slots/{id}` - Update time slot
- `DELETE /admin/api/time-slots/{id}` - Delete time slot

**Fields:**
- startTime (required) - LocalTime format
- endTime (required) - LocalTime format
- durationMinutes (required, min: 1)
- slotName (optional) - e.g., "Lecture 1", "Lab Session"
- isBreak (default: false)
- isActive (default: true)

---

## Security Configuration

### Authentication
- JWT-based authentication
- Token stored in localStorage
- Automatic token injection in API requests
- Token expiry handling with auto-logout

### Authorization
- **Public Endpoints:** `/auth/**` (login, register, verify-email, forgot-password, reset-password)
- **Teacher Endpoints:** `/api/teachers/**` (requires TEACHER or ADMIN role)
- **Admin Endpoints:** `/admin/**` (requires ADMIN role only)

### CORS Configuration
- Allows all origins (for development)
- Allows all methods (GET, POST, PUT, DELETE, PATCH, OPTIONS)
- Allows all headers
- Credentials enabled

---

## Database Schema

### Entity Relationships
```
DepartmentEntity (1) ----< (N) TeacherEntity
DepartmentEntity (1) ----< (N) CourseEntity
DepartmentEntity (1) ----< (N) ClassRoom
DepartmentEntity (1) ----< (N) Division

AcademicYear (1) ----< (N) Division
AcademicYear (1) ----< (N) TimetableEntry

Division (1) ----< (N) TimetableEntry
CourseEntity (1) ----< (N) TimetableEntry
TeacherEntity (1) ----< (N) TimetableEntry
ClassRoom (1) ----< (N) TimetableEntry
TimeSlot (1) ----< (N) TimetableEntry

TeacherEntity (N) ----< (N) CourseEntity (via teacher_courses join table)
```

---

## Frontend Updates

### Teacher Timetable Display (✅ Updated)
**File:** `Frontend/src/pages/teacher/TimetablePage.tsx`

**Changes:**
- Added `year` field to timetable data structure
- Display academic year (FE, SE, TE, BE) alongside division
- Updated UI to show both year and division badges
- Sample data includes year information for each class

**Display Format:**
```
Subject Name
Room Number
[Year Badge] [Division Badge]
```

Example: `SY` `Div A` for Second Year Division A

---

## API Response Format

### Success Response
```json
{
  "id": 1,
  "name": "Computer Engineering",
  "code": "COMP",
  "headOfDepartment": "Dr. John Doe",
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-01T10:00:00"
}
```

### Error Response
```json
{
  "timestamp": "2024-01-01T10:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Department not found with id: 1",
  "path": "/admin/api/departments/1"
}
```

---

## Testing the API

### Using Postman
1. Import the collection: `SamaySetu_Postman_Collection.json`
2. Set environment variables:
   - `base_url`: http://localhost:8083
   - `jwt_token`: (obtained after login)

### Sample Requests

#### 1. Login (Get JWT Token)
```http
POST http://localhost:8083/auth/login
Content-Type: application/json

{
  "email": "teacher@mitaoe.ac.in",
  "password": "password123"
}
```

#### 2. Create Department (Admin)
```http
POST http://localhost:8083/admin/api/departments
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "name": "Computer Engineering",
  "code": "COMP",
  "headOfDepartment": "Dr. John Doe"
}
```

#### 3. Get All Departments
```http
GET http://localhost:8083/admin/api/departments
Authorization: Bearer {jwt_token}
```

#### 4. Create Time Slot
```http
POST http://localhost:8083/admin/api/time-slots
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "startTime": "09:00:00",
  "endTime": "10:00:00",
  "durationMinutes": 60,
  "slotName": "Lecture 1",
  "isBreak": false,
  "isActive": true
}
```

---

## Running the Application

### Backend
```bash
cd Backend
mvnw spring-boot:run
```
Server runs on: http://localhost:8083

### Frontend
```bash
cd Frontend
npm install
npm run dev
```
Frontend runs on: http://localhost:5173

---

## Database Configuration

**File:** `Backend/src/main/resources/application.properties`

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/samaysetu
spring.datasource.username=root
spring.datasource.password=root
spring.jpa.hibernate.ddl-auto=update
```

**Note:** Ensure MySQL is running and database `samaysetu` exists.

---

## Next Steps (Phase 2)

1. **Timetable Generation**
   - Implement timetable generation algorithm
   - Handle constraints (teacher availability, room capacity, etc.)
   - Conflict detection and resolution

2. **Teacher Availability**
   - CRUD operations for teacher availability
   - Integration with timetable generation

3. **Timetable Entry Management**
   - CRUD operations for timetable entries
   - Bulk operations for timetable management
   - Export/Import functionality

4. **Reports and Analytics**
   - Teacher workload reports
   - Room utilization reports
   - Timetable conflict reports

5. **Advanced Features**
   - Drag-and-drop timetable editor
   - Real-time conflict detection
   - Notification system
   - Audit logs

---

## Summary

✅ **7 Complete REST APIs** with full CRUD operations
✅ **JWT Authentication** with email verification
✅ **Role-based Authorization** (TEACHER, ADMIN)
✅ **CORS Configuration** for frontend integration
✅ **Error Handling** with proper HTTP status codes
✅ **Data Validation** using Jakarta Validation
✅ **Teacher Timetable UI** updated to show academic year
✅ **Complete Entity Relationships** with proper foreign keys
✅ **Security Configuration** with protected endpoints

**Backend Implementation: 100% Complete for Phase 1**
**Frontend Integration: Ready for API consumption**
