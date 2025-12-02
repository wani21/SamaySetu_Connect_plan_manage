# 📮 SamaySetu Postman Collection

Complete API testing collection for SamaySetu Timetable Management System

---

## 📦 What's Included

### Files:
1. **SamaySetu_Postman_Collection.json** - 13 API endpoints
2. **SamaySetu_Postman_Environment.json** - Environment variables
3. **POSTMAN_QUICK_IMPORT.md** - 2-minute import guide
4. **POSTMAN_COLLECTION_GUIDE.md** - Complete documentation
5. **POSTMAN_COLLECTION_README.md** - This file

---

## ⚡ Quick Start

### 1. Import (1 minute)
```
1. Open Postman
2. Import → SamaySetu_Postman_Collection.json
3. Import → SamaySetu_Postman_Environment.json
4. Select "SamaySetu Local Environment"
```

### 2. Test (2 minutes)
```
1. Start backend: mvnw.cmd spring-boot:run
2. Run: Authentication → Register Teacher
3. Check email & verify
4. Run: Authentication → Login
5. JWT token auto-saved!
```

---

## 📁 Collection Structure

```
SamaySetu Timetable API (13 endpoints)
│
├── 🔐 Authentication (6)
│   ├── Generate Password Hash
│   ├── Register Teacher
│   ├── Verify Email
│   ├── Login (auto-saves JWT)
│   ├── Forgot Password
│   └── Reset Password
│
├── 👨‍🏫 Teachers (1)
│   └── Create Teacher
│
├── 🏢 Departments - Admin (2)
│   ├── Create Department
│   └── Get All Departments
│
├── 📚 Courses - Admin (1)
│   └── Create Course
│
├── 🏫 Rooms - Admin (1)
│   └── Create Room
│
├── 📅 Academic Years - Admin (1)
│   └── Create Academic Year
│
└── 🎓 Divisions - Admin (1)
    └── Create Division
```

---

## 🎯 Features

### ✅ Automatic JWT Handling
- Login saves token automatically
- Token used for all protected endpoints
- No manual copy-paste needed

### ✅ Environment Variables
- `{{base_url}}` - API base URL
- `{{jwt_token}}` - Auto-set after login
- `{{verification_token}}` - For email verification
- `{{reset_token}}` - For password reset

### ✅ Test Scripts
- Validates responses
- Saves tokens automatically
- Checks status codes

### ✅ Complete Examples
- All request bodies included
- Sample data provided
- Enum values documented

---

## 🔐 Authentication Flow

```
Register → Verify Email → Login → Protected Endpoints
   ↓           ↓           ↓            ↓
 Email      Token      JWT Token    Auto-used
 Sent       Check      Saved        in requests
```

---

## 📊 Endpoint Summary

| Category | Endpoints | Auth Required | Role |
|----------|-----------|---------------|------|
| Authentication | 6 | No | Public |
| Teachers | 1 | Yes | TEACHER |
| Departments | 2 | Yes | ADMIN |
| Courses | 1 | Yes | ADMIN |
| Rooms | 1 | Yes | ADMIN |
| Academic Years | 1 | Yes | ADMIN |
| Divisions | 1 | Yes | ADMIN |

**Total: 13 endpoints**

---

## 🧪 Testing Scenarios

### Scenario 1: New User Registration
```
1. Register Teacher
2. Check email
3. Verify Email
4. Login
5. Create Teacher
```

### Scenario 2: Password Reset
```
1. Forgot Password
2. Check email
3. Reset Password
4. Login with new password
```

### Scenario 3: Admin Setup
```
1. Login as Admin
2. Create Department
3. Create Academic Year
4. Create Course
5. Create Room
6. Create Division
```

---

## 🎨 Sample Requests

### Register
```json
{
  "name": "Test Teacher",
  "employeeId": "TEST001",
  "email": "test@mitaoe.ac.in",
  "password": "test123",
  "phone": "1234567890"
}
```

### Login
```json
{
  "email": "test@mitaoe.ac.in",
  "password": "test123"
}
```

### Create Department
```json
{
  "name": "Computer Science",
  "code": "CS",
  "headOfDepartment": "Dr. John Smith"
}
```

---

## 🔧 Configuration

### Base URL
```
http://localhost:8083
```

### Authorization
```
Type: Bearer Token
Token: {{jwt_token}}
```

### Headers
```
Content-Type: application/json
```

---

## 📝 Variables Reference

### Collection Variables:
- `base_url` - API endpoint
- `jwt_token` - Authentication token
- `verification_token` - Email verification
- `reset_token` - Password reset

### Environment Variables:
- `test_email` - Test user email
- `test_password` - Test user password
- `department_id` - Created department ID
- `academic_year_id` - Created academic year ID

---

## 🐛 Troubleshooting

### Backend Not Running
```
Error: Could not send request
Solution: Start backend with mvnw.cmd spring-boot:run
```

### 403 Forbidden
```
Error: 403 Forbidden
Solutions:
- Login to get JWT token
- Verify email first
- Check correct role (TEACHER/ADMIN)
```

### Email Not Verified
```
Error: "Email not verified..."
Solution: Run "Verify Email" request with token from email
```

### Token Expired
```
Error: Invalid token
Solution: Login again to get new JWT token (10h expiry)
```

---

## 📚 Documentation

### Quick Start:
- **POSTMAN_QUICK_IMPORT.md** - 2-minute setup

### Complete Guide:
- **POSTMAN_COLLECTION_GUIDE.md** - Full documentation

### API Reference:
- **API_QUICK_REFERENCE.md** - Endpoint reference
- **AUTHENTICATION_GUIDE.md** - Auth details
- **EMAIL_VERIFICATION_SETUP.md** - Email setup

---

## ✅ Prerequisites

Before using collection:
- [ ] Backend running on port 8083
- [ ] MySQL database running
- [ ] Email configured in application.properties
- [ ] Postman installed

---

## 🎯 Quick Test

### Test Backend Connection:
```
1. Open: Authentication → Login
2. Click: Send
3. Should get error (expected - no user yet)
4. ✅ Backend is working!
```

### Test Complete Flow:
```
1. Register Teacher
2. Verify Email (check inbox)
3. Login (token auto-saved)
4. Create Teacher (uses token)
5. ✅ All working!
```

---

## 🚀 Advanced Usage

### Collection Runner
```
1. Click collection name
2. Click "Run"
3. Select requests
4. Click "Run SamaySetu Timetable API"
```

### Export & Share
```
1. Right-click collection
2. Export
3. Share JSON file with team
```

### Custom Scripts
```javascript
// Pre-request Script
pm.environment.set("timestamp", Date.now());

// Test Script
pm.test("Success", () => {
    pm.response.to.have.status(200);
});
```

---

## 📊 Statistics

- **Total Endpoints:** 13
- **Public Endpoints:** 6
- **Protected Endpoints:** 7
- **Admin Endpoints:** 6
- **Teacher Endpoints:** 1
- **Test Scripts:** 6
- **Environment Variables:** 8

---

## 🎉 Benefits

✅ **Complete Coverage** - All API endpoints included  
✅ **Auto JWT Handling** - No manual token management  
✅ **Test Scripts** - Automatic validation  
✅ **Documentation** - Detailed descriptions  
✅ **Examples** - Sample data provided  
✅ **Variables** - Easy configuration  
✅ **Team Ready** - Share with colleagues  

---

## 📞 Support

### Issues?
1. Check backend is running
2. Verify email configuration
3. Review error messages
4. Check documentation

### Documentation:
- Quick Import: `POSTMAN_QUICK_IMPORT.md`
- Full Guide: `POSTMAN_COLLECTION_GUIDE.md`
- API Reference: `API_QUICK_REFERENCE.md`

---

## 🎊 Ready to Test!

Your complete Postman collection is ready with:
- 13 API endpoints
- Automatic JWT handling
- Environment variables
- Test scripts
- Complete documentation

**Import and start testing in 2 minutes!**

See `POSTMAN_QUICK_IMPORT.md` for step-by-step guide.

Happy Testing! 🚀
