# 📮 Postman Collection Guide - SamaySetu API

## 📦 What's Included

### Files Created:
1. **SamaySetu_Postman_Collection.json** - Complete API collection
2. **SamaySetu_Postman_Environment.json** - Environment variables
3. **POSTMAN_COLLECTION_GUIDE.md** - This guide

---

## 🚀 Quick Start (3 Steps)

### Step 1: Import Collection

1. Open Postman
2. Click **Import** button (top left)
3. Drag and drop `SamaySetu_Postman_Collection.json`
4. Click **Import**

### Step 2: Import Environment

1. Click **Import** again
2. Drag and drop `SamaySetu_Postman_Environment.json`
3. Click **Import**
4. Select "SamaySetu Local Environment" from environment dropdown (top right)

### Step 3: Start Testing!

1. Ensure backend is running: `http://localhost:8083`
2. Open collection: **SamaySetu Timetable API**
3. Start with **Authentication** folder

---

## 📁 Collection Structure

```
SamaySetu Timetable API
├── Authentication (6 endpoints)
│   ├── 1. Generate Password Hash
│   ├── 2. Register Teacher
│   ├── 3. Verify Email
│   ├── 4. Login
│   ├── 5. Forgot Password
│   └── 6. Reset Password
├── Teachers (1 endpoint)
│   └── Create Teacher
├── Departments (Admin) (2 endpoints)
│   ├── Create Department
│   └── Get All Departments
├── Courses (Admin) (1 endpoint)
│   └── Create Course
├── Rooms (Admin) (1 endpoint)
│   └── Create Room
├── Academic Years (Admin) (1 endpoint)
│   └── Create Academic Year
└── Divisions (Admin) (1 endpoint)
    └── Create Division
```

**Total: 13 API endpoints**

---

## 🔐 Authentication Flow

### Complete Test Sequence:

#### 1. Register Teacher
```
POST /auth/register
```
- Uses college email (@mitaoe.ac.in)
- Sends verification email
- Account inactive until verified

#### 2. Check Email
- Open your email inbox
- Find verification email from "SamaySetu Admin"
- Copy the token from the URL

#### 3. Verify Email
```
GET /auth/verify-email?token=YOUR_TOKEN
```
- Paste token in `verification_token` variable
- Activates account

#### 4. Login
```
POST /auth/login
```
- **Automatic:** JWT token saved to `jwt_token` variable
- Token used for all protected endpoints

#### 5. Test Protected Endpoint
```
POST /api/teachers
```
- Uses JWT token automatically
- Creates new teacher

---

## 🎯 Testing Scenarios

### Scenario 1: Complete Registration Flow

1. **Generate Password Hash** (Optional)
   - Useful for manual database inserts
   
2. **Register Teacher**
   - Email: `test@mitaoe.ac.in`
   - Password: `test123`
   
3. **Verify Email**
   - Get token from email
   - Paste in `verification_token` variable
   - Run request
   
4. **Login**
   - JWT token auto-saved
   - Ready for protected endpoints

---

### Scenario 2: Password Reset Flow

1. **Forgot Password**
   - Enter email
   - Check email for reset link
   
2. **Copy Reset Token**
   - From email link
   - Save to `reset_token` variable
   
3. **Reset Password**
   - Enter new password
   - Token validated
   
4. **Login with New Password**
   - Test new credentials

---

### Scenario 3: Admin Operations

**Prerequisites:** Login as ADMIN user

1. **Create Department**
   - Note the department ID
   
2. **Create Academic Year**
   - Note the academic year ID
   
3. **Create Course**
   - Link to department
   
4. **Create Room**
   - Link to department
   
5. **Create Division**
   - Link to department and academic year

---

## 🔧 Environment Variables

### Automatic Variables:
- `jwt_token` - Auto-set after login
- `base_url` - API base URL

### Manual Variables:
- `verification_token` - Copy from email
- `reset_token` - Copy from reset email
- `test_email` - Your test email
- `test_password` - Your test password
- `department_id` - Created department ID
- `academic_year_id` - Created academic year ID

### How to Set Variables:

**Option 1: In Environment**
1. Click environment dropdown (top right)
2. Click eye icon
3. Edit values

**Option 2: In Request**
1. Use `{{variable_name}}` in request
2. Hover over variable
3. Click to edit

---

## 📝 Request Examples

### Authentication Requests

#### Register
```json
{
  "name": "Test Teacher",
  "employeeId": "TEST001",
  "email": "test@mitaoe.ac.in",
  "phone": "1234567890",
  "password": "test123",
  "specialization": "Computer Science",
  "departmentId": 1
}
```

#### Login
```json
{
  "email": "test@mitaoe.ac.in",
  "password": "test123"
}
```

#### Forgot Password
```json
{
  "email": "test@mitaoe.ac.in"
}
```

#### Reset Password
```json
{
  "token": "{{reset_token}}",
  "newPassword": "newPassword123"
}
```

---

### Admin Requests

#### Create Department
```json
{
  "name": "Computer Science",
  "code": "CS",
  "headOfDepartment": "Dr. John Smith"
}
```

#### Create Course
```json
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

#### Create Room
```json
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

#### Create Academic Year
```json
{
  "yearName": "2024-25",
  "startDate": "2024-07-01",
  "endDate": "2025-06-30",
  "isCurrent": true
}
```

#### Create Division
```json
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

## 🎨 Enum Values Reference

### CourseType
- `THEORY`
- `LAB`

### RoomType
- `CLASSROOM`
- `LAB`
- `AUDITORIUM`

### Semester
- `SEM_1`, `SEM_2`, `SEM_3`, `SEM_4`
- `SEM_5`, `SEM_6`, `SEM_7`, `SEM_8`

### DayOfWeek
- `MONDAY`, `TUESDAY`, `WEDNESDAY`
- `THURSDAY`, `FRIDAY`, `SATURDAY`

---

## 🧪 Testing Tips

### 1. Test Scripts Included
- Login automatically saves JWT token
- Registration validates response
- Email verification checks success

### 2. Use Variables
```
{{base_url}}/auth/login
{{jwt_token}} in Authorization header
{{verification_token}} in query params
```

### 3. Check Response
- Status codes (200, 400, 403)
- Response body
- Headers

### 4. Sequential Testing
Run requests in order:
1. Register → 2. Verify → 3. Login → 4. Protected endpoints

---

## 🔒 Authorization

### Public Endpoints (No Auth):
- `/auth/**` - All authentication endpoints

### Protected Endpoints (JWT Required):
- `/api/teachers` - TEACHER role
- `/admin/**` - ADMIN role

### How JWT Works:
1. Login returns JWT token
2. Token auto-saved to `jwt_token` variable
3. Collection uses Bearer token authentication
4. Token sent in `Authorization: Bearer {{jwt_token}}` header

---

## 🐛 Troubleshooting

### Issue: 403 Forbidden

**Causes:**
1. No JWT token
2. Token expired (10 hours)
3. Wrong role (TEACHER vs ADMIN)
4. Email not verified

**Solutions:**
1. Login again to get new token
2. Check `jwt_token` variable is set
3. Verify email before login
4. Use correct role for endpoint

---

### Issue: Email Not Verified

**Error:** `"Email not verified. Please check your email..."`

**Solution:**
1. Check email inbox (and spam)
2. Copy verification token
3. Run "Verify Email" request
4. Then login

---

### Issue: Invalid Token

**Error:** `"Invalid verification token"` or `"Token expired"`

**Solutions:**
1. Register again (generates new token)
2. Check token copied correctly
3. Verify token not expired (24h for verification, 1h for reset)

---

### Issue: College Email Required

**Error:** `"Only college email (@mitaoe.ac.in) is allowed"`

**Solution:**
- Use email ending with `@mitaoe.ac.in`
- Example: `yourname@mitaoe.ac.in`

---

## 📊 Response Codes

| Code | Meaning | Common Causes |
|------|---------|---------------|
| 200 | Success | Request completed successfully |
| 400 | Bad Request | Invalid data, validation failed |
| 403 | Forbidden | No auth, wrong role, email not verified |
| 404 | Not Found | Resource doesn't exist |
| 500 | Server Error | Backend error, check logs |

---

## 🎯 Quick Test Checklist

### Before Testing:
- [ ] Backend running on port 8083
- [ ] MySQL database running
- [ ] Email configured in application.properties
- [ ] Collection imported
- [ ] Environment selected

### Test Authentication:
- [ ] Register with college email
- [ ] Receive verification email
- [ ] Verify email successfully
- [ ] Login and get JWT token
- [ ] Token auto-saved to variable

### Test Protected Endpoints:
- [ ] Create teacher (TEACHER role)
- [ ] Create department (ADMIN role)
- [ ] Get all departments (ADMIN role)

### Test Password Reset:
- [ ] Request password reset
- [ ] Receive reset email
- [ ] Reset password successfully
- [ ] Login with new password

---

## 🚀 Advanced Features

### 1. Pre-request Scripts
Add to request "Pre-request Script" tab:
```javascript
// Auto-generate timestamp
pm.environment.set("timestamp", new Date().toISOString());

// Auto-generate employee ID
pm.environment.set("employeeId", "EMP" + Date.now());
```

### 2. Test Scripts
Add to request "Tests" tab:
```javascript
// Validate response
pm.test("Status is 200", function () {
    pm.response.to.have.status(200);
});

// Save response data
var jsonData = pm.response.json();
pm.environment.set("department_id", jsonData.id);
```

### 3. Collection Runner
1. Click collection name
2. Click "Run"
3. Select requests to run
4. Click "Run SamaySetu Timetable API"
5. View results

---

## 📚 Additional Resources

- **API Documentation:** `API_QUICK_REFERENCE.md`
- **Email Setup:** `QUICK_EMAIL_SETUP.md`
- **Authentication Guide:** `AUTHENTICATION_GUIDE.md`
- **Email Verification:** `EMAIL_VERIFICATION_SETUP.md`

---

## 💡 Pro Tips

1. **Save Tokens:** Use environment variables for tokens
2. **Test Order:** Follow numbered sequence in Authentication folder
3. **Check Email:** Always check spam folder for verification emails
4. **Admin User:** Create first admin via database (see docs)
5. **Token Expiry:** JWT tokens expire after 10 hours
6. **Variables:** Use `{{variable}}` syntax in requests
7. **Collection Runner:** Run all tests automatically
8. **Export:** Share collection with team members

---

## 🎉 You're Ready!

Your Postman collection is complete with:
- ✅ 13 API endpoints
- ✅ Automatic JWT token handling
- ✅ Environment variables
- ✅ Test scripts
- ✅ Complete documentation

Start testing by running the Authentication requests in order!

---

## 📞 Need Help?

- Check response body for error messages
- Review backend console logs
- Verify email configuration
- Ensure database is running
- Check JWT token is set after login

Happy Testing! 🚀
