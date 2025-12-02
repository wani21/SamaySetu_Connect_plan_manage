# API Endpoints Reference - SamaySetu Connect

## 🌐 Base URL
```
http://localhost:8083/api
```

---

## 📚 Department Endpoints

### Create Department
```http
POST /api/departments
Content-Type: application/json

{
  "name": "Computer Science",
  "code": "CS",
  "headOfDepartment": "Dr. John Smith"
}
```

### Get All Departments
```http
GET /api/departments
```

---

## 👨‍🏫 Teacher Endpoints

### Create Teacher
```http
POST /api/teachers
Content-Type: application/json

{
  "name": "Prof. Jane Doe",
  "employeeId": "EMP001",
  "email": "jane.doe@college.edu",
  "phone": "1234567890",
  "weeklyHoursLimit": 25,
  "specialization": "Data Structures, Algorithms",
  "isActive": true,
  "department": {
    "id": 1
  }
}
```

---

## 📖 Course Endpoints

### Create Course
```http
POST /api/courses
Content-Type: application/json

{
  "name": "Data Structures",
  "code": "CS201",
  "courseType": "THEORY",
  "credits": 4,
  "hoursPerWeek": 4,
  "semester": "SEM_3",
  "description": "Introduction to data structures",
  "isActive": true,
  "department": {
    "id": 1
  }
}
```

---

## 🏫 Room Endpoints

### Create Room
```http
POST /api/rooms
Content-Type: application/json

{
  "name": "CS Lab 1",
  "roomNumber": "CS-101",
  "capacity": 60,
  "roomType": "LAB",
  "hasProjector": true,
  "hasAc": true,
  "equipment": "60 computers, projector, whiteboard",
  "isActive": true,
  "department": {
    "id": 1
  }
}
```

---

## 📅 Academic Year Endpoints

### Create Academic Year
```http
POST /api/academic-years
Content-Type: application/json

{
  "yearName": "2024-25",
  "startDate": "2024-07-01",
  "endDate": "2025-06-30",
  "isCurrent": true
}
```

---

## 🎓 Division Endpoints

### Create Division
```http
POST /api/divisions
Content-Type: application/json

{
  "name": "A",
  "year": 2,
  "branch": "Computer Science",
  "totalStudents": 60,
  "isActive": true,
  "department": {
    "id": 1
  },
  "academicYear": {
    "id": 1
  }
}
```

---

## 📋 Enum Values Reference

### CourseType
- `THEORY`
- `LAB`

### RoomType
- `CLASSROOM`
- `LAB`
- `AUDITORIUM`

### Semester
- `SEM_1` through `SEM_8`

### DayOfWeek
- `MONDAY`
- `TUESDAY`
- `WEDNESDAY`
- `THURSDAY`
- `FRIDAY`
- `SATURDAY`

---

## 🔍 Response Format

### Success Response
```json
{
  "message": "Entity added successfully"
}
```

### Error Response
```json
{
  "timestamp": "2024-10-10T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/departments"
}
```

---

## 🧪 Testing with cURL

### Create Department
```bash
curl -X POST http://localhost:8083/api/departments \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Computer Science",
    "code": "CS",
    "headOfDepartment": "Dr. John Smith"
  }'
```

### Get All Departments
```bash
curl -X GET http://localhost:8083/api/departments
```

---

## 📝 Notes

1. All POST endpoints require `Content-Type: application/json` header
2. All entities support validation - invalid data will return 400 Bad Request
3. Foreign key relationships use nested objects with `id` field
4. Date format: `YYYY-MM-DD`
5. Boolean fields: `true` or `false`
