# 🚀 API Quick Reference - SamaySetu Authentication

## 📋 All Endpoints

### 1️⃣ Register (College Email Only)
```http
POST http://localhost:8083/auth/register
Content-Type: application/json

{
  "name": "John Doe",
  "employeeId": "EMP123",
  "email": "john.doe@mitaoe.ac.in",
  "password": "securePass123",
  "phone": "1234567890",
  "specialization": "Computer Science"
}
```
✅ Success: `"Registration successful! Please check your college email..."`  
❌ Error: `"Only college email (@mitaoe.ac.in) is allowed"`

---

### 2️⃣ Verify Email
```http
GET http://localhost:8083/auth/verify-email?token=YOUR_TOKEN
```
✅ Success: `"Email verified successfully! You can now login."`  
❌ Error: `"Verification token has expired"`

---

### 3️⃣ Login (Requires Verified Email)
```http
POST http://localhost:8083/auth/login
Content-Type: application/json

{
  "email": "john.doe@mitaoe.ac.in",
  "password": "securePass123"
}
```
✅ Success:
```json
{
  "email": "john.doe@mitaoe.ac.in",
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "role": "TEACHER"
}
```
❌ Errors:
- `"Email not verified. Please check your email..."`
- `"Account is not active..."`
- `"Email or password is incorrect"`

---

### 4️⃣ Forgot Password
```http
POST http://localhost:8083/auth/forgot-password
Content-Type: application/json

{
  "email": "john.doe@mitaoe.ac.in"
}
```
✅ Success: `"Password reset link has been sent to your email."`  
❌ Error: `"Email not found"`

---

### 5️⃣ Reset Password
```http
POST http://localhost:8083/auth/reset-password
Content-Type: application/json

{
  "token": "YOUR_RESET_TOKEN",
  "newPassword": "newSecurePass456"
}
```
✅ Success: `"Password reset successfully! You can now login..."`  
❌ Error: `"Password reset token has expired"`

---

### 6️⃣ Generate Password Hash (Utility)
```http
POST http://localhost:8083/auth/
Content-Type: application/json

{
  "password": "yourPassword"
}
```
✅ Returns: BCrypt hashed password

---

## 🔐 Protected Endpoints (Require JWT)

### Get Teachers
```http
GET http://localhost:8083/api/teachers
Authorization: Bearer YOUR_JWT_TOKEN
```

### Create Teacher (Admin)
```http
POST http://localhost:8083/api/teachers
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json

{
  "name": "Jane Smith",
  "employeeId": "EMP456",
  "email": "jane@mitaoe.ac.in",
  "password": "password123"
}
```

---

## 📧 Email Setup (5 Minutes)

1. **Get Gmail App Password:**
   - Visit: https://myaccount.google.com/apppasswords
   - Generate password for "Mail"

2. **Update application.properties:**
```properties
spring.mail.username=your-email@mitaoe.ac.in
spring.mail.password=your-16-char-app-password
```

3. **Restart backend**

---

## 🧪 Complete Test Flow

```bash
# 1. Register
POST /auth/register
{
  "name": "Test User",
  "employeeId": "TEST001",
  "email": "test@mitaoe.ac.in",
  "password": "test123",
  "phone": "1234567890"
}

# 2. Check email & get token

# 3. Verify email
GET /auth/verify-email?token=TOKEN_FROM_EMAIL

# 4. Login
POST /auth/login
{
  "email": "test@mitaoe.ac.in",
  "password": "test123"
}

# 5. Use JWT token
GET /api/teachers
Authorization: Bearer JWT_TOKEN_FROM_LOGIN

# 6. Test password reset
POST /auth/forgot-password
{
  "email": "test@mitaoe.ac.in"
}

# 7. Check email & get reset token

# 8. Reset password
POST /auth/reset-password
{
  "token": "RESET_TOKEN_FROM_EMAIL",
  "newPassword": "newPass123"
}

# 9. Login with new password
POST /auth/login
{
  "email": "test@mitaoe.ac.in",
  "password": "newPass123"
}
```

---

## ⚡ Quick Troubleshooting

| Issue | Solution |
|-------|----------|
| Email not received | Check spam, verify app password |
| "Only college email allowed" | Use `@mitaoe.ac.in` email |
| "Email not verified" | Click verification link in email |
| "Token expired" | Register again or request new reset |
| 403 Forbidden | Check JWT token in Authorization header |

---

## 📚 Documentation Files

- `QUICK_EMAIL_SETUP.md` - 5-minute setup guide
- `EMAIL_VERIFICATION_SETUP.md` - Complete detailed guide
- `IMPLEMENTATION_SUMMARY.md` - What's been implemented
- `AUTHENTICATION_GUIDE.md` - Full authentication docs
- `API_QUICK_REFERENCE.md` - This file

---

## 🎯 Key Features

✅ College email validation (`@mitaoe.ac.in`)  
✅ Email verification required  
✅ Password reset via email  
✅ JWT authentication  
✅ Role-based access control  
✅ BCrypt password hashing  
✅ Token expiry (24h verification, 1h reset)  
✅ Automated email notifications  

---

## 🔒 Security Notes

- Verification token: 24 hours
- Reset token: 1 hour
- Tokens are single-use
- Cannot login without email verification
- Passwords are BCrypt hashed
- JWT tokens expire after 10 hours

---

## 📞 Need Help?

Check the detailed guides:
- Setup issues → `QUICK_EMAIL_SETUP.md`
- API details → `EMAIL_VERIFICATION_SETUP.md`
- Implementation → `IMPLEMENTATION_SUMMARY.md`
