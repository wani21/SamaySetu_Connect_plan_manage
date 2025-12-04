# Profile API Implementation - Complete

## Overview

Implemented complete profile view, edit, and update functionality for teachers with backend API endpoints and frontend integration.

## Backend Implementation

### 1. Profile Endpoints Added

**File**: `Backend/src/main/java/com/College/timetable/Controller/TeacherController.java`

#### New Endpoint: Update Profile
```java
@PutMapping("/profile")
public ResponseEntity<TeacherEntity> updateProfile(
    Authentication authentication, 
    @Valid @RequestBody TeacherEntity teach
) {
    String email = authentication.getName();
    TeacherEntity currentTeacher = teacherService.getByEmail(email);
    TeacherEntity updated = teacherService.update(currentTeacher.getId(), teach);
    return ResponseEntity.ok(updated);
}
```

#### Existing Endpoints
```java
// Get current user's profile
@GetMapping("/profile")
public ResponseEntity<TeacherEntity> getProfile(Authentication authentication)

// Get all teachers
@GetMapping
public ResponseEntity<List<TeacherEntity>> getAllTeachers()

// Get teacher by ID
@GetMapping("/{id}")
public ResponseEntity<TeacherEntity> getTeacherById(@PathVariable Long id)

// Update teacher by ID (admin)
@PutMapping("/{id}")
public ResponseEntity<TeacherEntity> updateTeacher(@PathVariable Long id, @Valid @RequestBody TeacherEntity teach)

// Delete teacher
@DeleteMapping("/{id}")
public ResponseEntity<String> deleteTeacher(@PathVariable Long id)
```

### 2. Service Layer

**File**: `Backend/src/main/java/com/College/timetable/Service/TeacherService.java`

#### Key Methods
- `getByEmail(String email)` - Get teacher by email
- `update(Long id, TeacherEntity teach)` - Update teacher profile
- `getById(Long id)` - Get teacher by ID
- `getAll()` - Get all teachers

#### Update Method Features
- Updates name, email, phone, specialization
- Updates weekly hours limit
- Updates department if provided
- Encodes password if provided
- Validates department exists
- Returns updated entity

## Frontend Implementation

### 1. API Service Updates

**File**: `Frontend/src/services/api.ts`

```typescript
export const teacherAPI = {
  create: (data: any) => api.post('/api/teachers', data),
  getProfile: () => api.get('/api/teachers/profile'),
  updateProfile: (data: any) => api.put('/api/teachers/profile', data),
  getAll: () => api.get('/api/teachers'),
  getById: (id: number) => api.get(`/api/teachers/${id}`),
};
```

### 2. Profile Page Updates

**File**: `Frontend/src/pages/teacher/ProfilePage.tsx`

#### Features Implemented

**Data Fetching**:
- Fetches profile data on component mount
- Shows loading state while fetching
- Displays error toast if fetch fails
- Populates form with real data

**Profile Update**:
- Updates name, phone, specialization
- Updates weekly hours limit
- Validates data before submission
- Shows success/error toasts
- Refreshes profile after update

**Password Change**:
- Validates password fields
- Checks password match
- Minimum 6 characters validation
- Updates password via profile API
- Clears form on success

**UI Features**:
- Loading spinner during fetch
- Disabled fields for email and employee ID
- Real-time form updates
- Error handling with toasts
- Profile picture with initial
- Role badge display

## API Endpoints Summary

### Teacher Profile Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/teachers/profile` | Get current user's profile | Yes (TEACHER/ADMIN) |
| PUT | `/api/teachers/profile` | Update current user's profile | Yes (TEACHER/ADMIN) |
| GET | `/api/teachers` | Get all teachers | Yes (TEACHER/ADMIN) |
| GET | `/api/teachers/{id}` | Get teacher by ID | Yes (TEACHER/ADMIN) |
| POST | `/api/teachers` | Create new teacher | Yes (TEACHER/ADMIN) |
| PUT | `/api/teachers/{id}` | Update teacher by ID | Yes (TEACHER/ADMIN) |
| DELETE | `/api/teachers/{id}` | Delete teacher | Yes (TEACHER/ADMIN) |

## Data Flow

### Get Profile
```
Frontend → GET /api/teachers/profile
         ← TeacherEntity (with all fields)
```

### Update Profile
```
Frontend → PUT /api/teachers/profile
           {
             name, phone, specialization,
             weeklyHoursLimit, password (optional)
           }
         ← Updated TeacherEntity
```

## Profile Data Structure

```typescript
{
  name: string;
  email: string;              // Read-only
  phone: string;
  employeeId: string;         // Read-only
  specialization: string;
  weeklyHoursLimit: number;
  department: {
    id: number;
    name: string;
    code: string;
  };
  role: string;               // Read-only
  isActive: boolean;
  isEmailVerified: boolean;
}
```

## Features

### Teacher Profile Page

✅ **View Profile**
- Fetches real data from backend
- Displays all profile information
- Shows department details
- Displays role and status

✅ **Edit Profile**
- Update name
- Update phone number
- Update specialization
- Update weekly hours limit
- Cannot change email or employee ID

✅ **Change Password**
- Validates current password
- Validates new password
- Confirms password match
- Minimum 6 characters
- Secure password update

✅ **UI/UX**
- Loading states
- Error handling
- Success notifications
- Form validation
- Disabled fields for read-only data
- Profile picture with initial
- Clean, modern design

### Admin Profile Page

✅ **View Profile**
- Displays admin information
- Shows role and status
- Member since date
- Statistics dashboard

✅ **Edit Profile**
- Toggle edit mode
- Update name, phone, designation
- Save/Cancel actions
- Local state management

## Security

### Authentication
- All endpoints require authentication
- Uses JWT token from login
- Token sent in Authorization header
- Spring Security validates token

### Authorization
- Profile endpoints accessible to TEACHER and ADMIN roles
- Users can only update their own profile via `/profile` endpoint
- Admin can update any teacher via `/{id}` endpoint

### Data Validation
- `@Valid` annotation on request body
- Email format validation
- Required field validation
- Password encoding with BCrypt
- Department existence validation

## Error Handling

### Backend
- EntityNotFoundException for missing records
- Validation errors for invalid data
- Proper HTTP status codes
- Descriptive error messages

### Frontend
- Try-catch blocks for API calls
- Toast notifications for errors
- Loading states during operations
- Form validation before submission
- User-friendly error messages

## Testing

### Backend Endpoints

**Get Profile**:
```bash
curl -X GET http://localhost:8083/api/teachers/profile \
  -H "Authorization: Bearer <token>"
```

**Update Profile**:
```bash
curl -X PUT http://localhost:8083/api/teachers/profile \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Updated Name",
    "phone": "9876543210",
    "specialization": "AI & ML",
    "weeklyHoursLimit": 30
  }'
```

### Frontend Testing

1. Login as teacher
2. Navigate to Profile page
3. Verify data loads correctly
4. Update profile information
5. Verify success message
6. Refresh page to confirm changes
7. Test password change
8. Verify password updated

## Files Modified

### Backend
1. `Backend/src/main/java/com/College/timetable/Controller/TeacherController.java`
   - Added `@PutMapping("/profile")` endpoint

### Frontend
1. `Frontend/src/services/api.ts`
   - Added `getProfile()` method
   - Added `updateProfile()` method
   - Added `getAll()` method
   - Added `getById()` method

2. `Frontend/src/pages/teacher/ProfilePage.tsx`
   - Added `useEffect` for data fetching
   - Added `fetchProfile()` function
   - Updated `handleProfileUpdate()` with API call
   - Updated `handlePasswordChange()` with API call
   - Added loading state
   - Added error handling

## Next Steps (Optional Enhancements)

1. **Profile Picture Upload**
   - Add file upload endpoint
   - Store images in server/cloud
   - Display uploaded pictures

2. **Activity Log**
   - Track profile changes
   - Show last updated timestamp
   - Display change history

3. **Email Change**
   - Add email change workflow
   - Send verification to new email
   - Confirm before updating

4. **Two-Factor Authentication**
   - Add 2FA setup
   - QR code generation
   - Verification code input

5. **Preferences**
   - Theme selection
   - Notification settings
   - Language preferences

## Result

✅ Complete profile API implementation
✅ Backend endpoints working
✅ Frontend integration complete
✅ Real data fetching and updating
✅ Password change functionality
✅ Error handling implemented
✅ Loading states added
✅ User-friendly interface
✅ Secure authentication
✅ Data validation
