# 🔐 Authentication & User Management Guide - SamaySetu

## 📋 Table of Contents
1. [User Roles](#user-roles)
2. [Registration Methods](#registration-methods)
3. [Authentication Flow](#authentication-flow)
4. [API Endpoints](#api-endpoints)
5. [Testing with Postman](#testing-with-postman)

---

## 👥 User Roles

Your system currently supports **2 roles**:

### 1. TEACHER (Default)
- Can access `/api/teachers` endpoints
- Default role for all registered users
- Manages their own timetable and availability

### 2. ADMIN
- Can access `/admin/**` endpoints
- Full system access
- Can manage all teachers, courses, departments, etc.

**Role-Based Access Control:**
```
/auth/**           → Public (no authentication required)
/api/teachers      → Requires ROLE_TEACHER
/admin/**          → Requires ROLE_ADMIN
All other /api/**  → Requires authentication
```

---

## 📝 Registration Methods

### Method 1: Self-Registration (NEW - Recommended)
Users can register themselves using the public registration endpoint.

**Endpoint:** `POST /auth/register`

**Request Body:**
```json
{
  "name": "John Doe",
  "employeeId": "EMP123",
  "email": "john.doe@college.edu",
  "phone": "1234567890",
  "password": "securePassword123",
  "specialization": "Computer Science",
  "departmentId": 1
}
```

**Response:**
```json
"Teacher registered successfully. Please wait for admin approval."
```

**Features:**
- ✅ Validates email uniqueness
- ✅ Validates employee ID uniqueness
- ✅ Automatically hashes password with BCrypt
- ✅ Sets default role as TEACHER
- ✅ Sets isActive = true (can be changed to false for admin approval)

---

### Method 2: Admin Registration
Admin can create teachers using the protected endpoint.

**Endpoint:** `POST /api/teachers`
**Requires:** ROLE_TEACHER or higher

**Request Body:**
```json
{
  "name": "Jane Smith",
  "employeeId": "EMP456",
  "email": "jane.smith@college.edu",
  "phone": "9876543210",
  "password": "password123",
  "weeklyHoursLimit": 30,
  "specialization": "Mathematics",
  "isActive": true,
  "role": "TEACHER",
  "department": {
    "id": 1
  }
}
```

---

### Method 3: Direct Database Insert
For initial setup or creating admin users.

```sql
USE samaysetu;

-- First, generate BCrypt hash using: POST /auth/ with {"password": "yourpassword"}

INSERT INTO teachers (
    name, employee_id, email, phone, weekly_hours_limit, 
    specialization, is_active, password, role, created_at, updated_at
)
VALUES (
    'Admin User',
    'ADMIN001',
    'admin@college.edu',
    '1234567890',
    40,
    'Administration',
    1,
    '$2a$10$YOUR_BCRYPT_HASH_HERE',
    'ADMIN',
    NOW(),
    NOW()
);
```

---

## 🔄 Authentication Flow

### 1. Registration Flow
```
User → POST /auth/register → System validates → Hash password → Save to DB → Success
```

### 2. Login Flow
```
User → POST /auth/login → Authenticate credentials → Generate JWT → Return token + role
```

### 3. Protected Request Flow
```
User → Request with JWT → JWT Filter validates → Extract user → Authorize → Process request
```

---

## 🌐 API Endpoints

### Public Endpoints (No Authentication)

#### 1. Generate Password Hash
```http
POST /auth/
Content-Type: application/json

{
  "password": "yourPassword"
}
```
**Response:** BCrypt hashed password string

---

#### 2. Register New Teacher
```http
POST /auth/register
Content-Type: application/json

{
  "name": "John Doe",
  "employeeId": "EMP123",
  "email": "john.doe@college.edu",
  "phone": "1234567890",
  "password": "securePassword123",
  "specialization": "Computer Science",
  "departmentId": 1
}
```
**Response:** Success message

---

#### 3. Login
```http
POST /auth/login
Content-Type: application/json

{
  "email": "john.doe@college.edu",
  "password": "securePassword123"
}
```
**Response:**
```json
{
  "email": "john.doe@college.edu",
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "role": "TEACHER"
}
```

---

### Protected Endpoints (Requires Authentication)

#### 4. Create Teacher (Admin/Teacher)
```http
POST /api/teachers
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json

{
  "name": "Jane Smith",
  "employeeId": "EMP456",
  "email": "jane.smith@college.edu",
  "password": "password123",
  "role": "TEACHER"
}
```

---

## 🧪 Testing with Postman

### Test 1: Register a New Teacher

**Step 1:** Register
```
POST http://localhost:8083/auth/register
Content-Type: application/json

{
  "name": "Test Teacher",
  "employeeId": "TEST001",
  "email": "test@college.edu",
  "phone": "1234567890",
  "password": "test123",
  "specialization": "Computer Science"
}
```

**Expected:** `"Teacher registered successfully..."`

---

### Test 2: Login

**Step 2:** Login with registered credentials
```
POST http://localhost:8083/auth/login
Content-Type: application/json

{
  "email": "test@college.edu",
  "password": "test123"
}
```

**Expected Response:**
```json
{
  "email": "test@college.edu",
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0QGNvbGxlZ2UuZWR1IiwiaWF0IjoxNzAxNjE...",
  "role": "TEACHER"
}
```

**Copy the token!**

---

### Test 3: Access Protected Endpoint

**Step 3:** Use the token to access protected endpoint
```
GET http://localhost:8083/api/teachers
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0QGNvbGxlZ2UuZWR1...
```

**In Postman:**
1. Go to **Authorization** tab
2. Select **Type: Bearer Token**
3. Paste your token
4. Send request

**Expected:** 200 OK with data

---

### Test 4: Create Admin User

**Step 4:** Create admin via database
```sql
-- First get hash
POST http://localhost:8083/auth/
{"password": "admin123"}

-- Then insert
INSERT INTO teachers (name, employee_id, email, password, role, is_active, created_at, updated_at)
VALUES ('Admin', 'ADMIN001', 'admin@college.edu', 'YOUR_HASH', 'ADMIN', 1, NOW(), NOW());
```

**Step 5:** Login as admin
```
POST http://localhost:8083/auth/login
{
  "email": "admin@college.edu",
  "password": "admin123"
}
```

---

## 🔒 Security Features

✅ **Password Hashing:** BCrypt with salt  
✅ **JWT Tokens:** 10-hour expiration  
✅ **Role-Based Access:** TEACHER and ADMIN roles  
✅ **Stateless Sessions:** No server-side session storage  
✅ **CORS Enabled:** For frontend integration  
✅ **Email Validation:** Prevents duplicate emails  
✅ **Employee ID Validation:** Prevents duplicate IDs  

---

## 🚀 Future Enhancements (Optional)

### 1. Email Verification
- Send verification email on registration
- Activate account only after email confirmation

### 2. Password Reset
- Forgot password endpoint
- Email-based password reset link

### 3. Account Approval
- Set `isActive = false` on registration
- Admin approves new registrations

### 4. Refresh Tokens
- Long-lived refresh tokens
- Short-lived access tokens

### 5. Additional Roles
- STUDENT role
- HOD (Head of Department) role
- COORDINATOR role

---

## 📝 Notes

1. **Default Role:** All self-registered users get `ROLE_TEACHER`
2. **Admin Creation:** First admin must be created via database
3. **Token Expiration:** JWT tokens expire after 10 hours
4. **Password Requirements:** Minimum 6 characters (can be enhanced)
5. **Active Status:** Currently set to `true` by default (can require admin approval)

---

## 🐛 Troubleshooting

### Issue: 403 Forbidden on /auth/register
**Solution:** Ensure `/auth/**` is in `permitAll()` in SecurityConfig

### Issue: "Email already registered"
**Solution:** Use a different email or check database for existing user

### Issue: "Employee ID already exists"
**Solution:** Use a unique employee ID

### Issue: Token not working
**Solution:** Ensure token is prefixed with "Bearer " in Authorization header

---

## 📞 Support

For issues or questions, check:
- SecurityConfig.java for access rules
- AuthController.java for endpoints
- TeacherService.java for business logic
