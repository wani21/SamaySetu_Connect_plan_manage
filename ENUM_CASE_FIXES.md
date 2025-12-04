# Enum Case Sensitivity Fixes

## Issue
Admin was getting logged out when creating courses, rooms, and other entities due to enum case mismatch between frontend and backend.

**Error Message:**
```
JSON parse error: Cannot deserialize value of type `com.College.timetable.Entity.CourseType` 
from String "theory": not one of the values accepted for Enum class: [THEORY, LAB]
```

## Root Cause
- Frontend was sending lowercase enum values: `"theory"`, `"lab"`, `"classroom"`, `"auditorium"`
- Backend expects uppercase enum values: `"THEORY"`, `"LAB"`, `"CLASSROOM"`, `"AUDITORIUM"`

## Backend Enums

### CourseType
```java
public enum CourseType {
    THEORY,
    LAB
}
```

### RoomType
```java
public enum RoomType {
    CLASSROOM,
    LAB,
    AUDITORIUM
}
```

## Fixes Applied

### 1. CoursesPage.tsx
**Submit Handler:**
```typescript
await courseAPI.create({
  ...formData,
  courseType: formData.courseType.toUpperCase(), // Convert to uppercase
  credits: parseInt(formData.credits),
  hoursPerWeek: parseInt(formData.hoursPerWeek),
  semester: parseInt(formData.semester),
  departmentId: parseInt(formData.departmentId),
});
```

**Display Logic:**
```typescript
// Before: course.courseType === 'lab'
// After:  course.courseType === 'LAB'

// Before: {course.courseType.toUpperCase()}
// After:  {course.courseType} // Already uppercase from backend
```

### 2. RoomsPage.tsx
**Submit Handler:**
```typescript
await roomAPI.create({
  ...formData,
  roomType: formData.roomType.toUpperCase(), // Convert to uppercase
  capacity: parseInt(formData.capacity),
  departmentId: parseInt(formData.departmentId),
});
```

**Helper Functions:**
```typescript
const getRoomTypeColor = (type: string) => {
  switch (type.toUpperCase()) { // Handle both cases
    case 'LAB': return 'bg-purple-100 text-purple-800';
    case 'AUDITORIUM': return 'bg-orange-100 text-orange-800';
    default: return 'bg-blue-100 text-blue-800';
  }
};

const getRoomTypeIcon = (type: string) => {
  switch (type.toUpperCase()) { // Handle both cases
    case 'LAB': return '🔬';
    case 'AUDITORIUM': return '🎭';
    default: return '🏫';
  }
};
```

**Display Logic:**
```typescript
// Before: {room.roomType.toUpperCase()}
// After:  {room.roomType} // Already uppercase from backend
```

## Form Select Options
The select options remain lowercase for better UX:
```html
<option value="theory">Theory</option>
<option value="lab">Lab</option>
```

The values are converted to uppercase only when submitting to the API.

## Testing Checklist
- ✅ Create course with "theory" type
- ✅ Create course with "lab" type
- ✅ Create room with "classroom" type
- ✅ Create room with "lab" type
- ✅ Create room with "auditorium" type
- ✅ Display existing courses with correct styling
- ✅ Display existing rooms with correct icons and colors
- ✅ No logout on validation errors
- ✅ Proper error messages displayed

## Additional Fix: Department and AcademicYear Relationships

### Issue
Backend was receiving `null` for `department_id` causing SQL constraint violations:
```
Column 'department_id' cannot be null
```

### Root Cause
Frontend was sending:
```json
{
  "departmentId": 1
}
```

But backend expects nested objects:
```json
{
  "department": {
    "id": 1
  }
}
```

### Fixes Applied

**CoursesPage:**
```typescript
await courseAPI.create({
  name: formData.name,
  code: formData.code,
  courseType: formData.courseType.toUpperCase(),
  credits: parseInt(formData.credits),
  hoursPerWeek: parseInt(formData.hoursPerWeek),
  semester: `SEM_${formData.semester}`, // Convert to enum format
  description: formData.description || null,
  department: {
    id: parseInt(formData.departmentId) // Nested object
  },
});
```

**RoomsPage:**
```typescript
await roomAPI.create({
  name: formData.name,
  roomNumber: formData.roomNumber,
  roomType: formData.roomType.toUpperCase(),
  capacity: parseInt(formData.capacity),
  hasProjector: formData.hasProjector,
  hasAc: formData.hasAc,
  equipment: formData.equipment || null,
  department: {
    id: parseInt(formData.departmentId) // Nested object
  },
});
```

**DivisionsPage:**
```typescript
await divisionAPI.create({
  name: formData.name,
  year: parseInt(formData.year),
  branch: formData.branch,
  totalStudents: parseInt(formData.totalStudents),
  department: {
    id: parseInt(formData.departmentId) // Nested object
  },
  academicYear: {
    id: parseInt(formData.academicYearId) // Nested object
  },
});
```

## Result
Admin can now successfully create courses, rooms, divisions, and other entities without getting logged out. All enum values are properly converted to uppercase and all relationships are sent as nested objects with IDs as expected by the backend.
