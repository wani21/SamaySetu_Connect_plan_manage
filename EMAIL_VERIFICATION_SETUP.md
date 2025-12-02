# 📧 Email Verification & Password Reset Setup Guide

## 🎯 Features Implemented

✅ **College Email Validation** - Only `@mitaoe.ac.in` emails allowed  
✅ **Email Verification** - Users must verify email before login  
✅ **Forgot Password** - Password reset via email  
✅ **Account Activation** - Auto-activated after email verification  
✅ **Token Expiry** - Verification (24h) and Reset (1h) tokens expire  

---

## 📧 Email Configuration

### Step 1: Setup Gmail App Password

1. Go to your Google Account: https://myaccount.google.com/
2. Navigate to **Security** → **2-Step Verification** (enable if not already)
3. Scroll down to **App passwords**
4. Select **Mail** and **Other (Custom name)**
5. Enter "SamaySetu" and click **Generate**
6. Copy the 16-character password

### Step 2: Update application.properties

Open `Backend/src/main/resources/application.properties` and update:

```properties
# Email Configuration
spring.mail.username=your-college-email@mitaoe.ac.in
spring.mail.password=your-16-char-app-password

# Application Base URL (change in production)
app.base-url=http://localhost:8083
```

**Important:** 
- Use your actual college email
- Use the App Password (not your regular Gmail password)
- In production, change `app.base-url` to your actual domain

---

## 🗄️ Database Migration

The new fields will be automatically added when you restart the application (thanks to `spring.jpa.hibernate.ddl-auto=update`).

**New fields added to `teachers` table:**
- `is_email_verified` (BOOLEAN)
- `verification_token` (VARCHAR)
- `verification_token_expiry` (TIMESTAMP)
- `password_reset_token` (VARCHAR)
- `password_reset_token_expiry` (TIMESTAMP)

---

## 🚀 API Endpoints

### 1. Register (Public)

**Endpoint:** `POST /auth/register`

**Request:**
```json
{
  "name": "John Doe",
  "employeeId": "EMP123",
  "email": "john.doe@mitaoe.ac.in",
  "phone": "1234567890",
  "password": "securePass123",
  "specialization": "Computer Science",
  "departmentId": 1
}
```

**Response:**
```
"Registration successful! Please check your college email to verify your account."
```

**What happens:**
1. Validates email ends with `@mitaoe.ac.in`
2. Checks for duplicate email/employee ID
3. Creates user with `isActive=false` and `isEmailVerified=false`
4. Generates verification token (24h expiry)
5. Sends verification email

---

### 2. Verify Email (Public)

**Endpoint:** `GET /auth/verify-email?token=YOUR_TOKEN`

**Response:**
```
"Email verified successfully! You can now login."
```

**What happens:**
1. Validates token exists and not expired
2. Sets `isEmailVerified=true` and `isActive=true`
3. Clears verification token
4. Sends welcome email

**Email Link Example:**
```
http://localhost:8083/auth/verify-email?token=a1b2c3d4-e5f6-7890-abcd-ef1234567890
```

---

### 3. Login (Public)

**Endpoint:** `POST /auth/login`

**Request:**
```json
{
  "email": "john.doe@mitaoe.ac.in",
  "password": "securePass123"
}
```

**Success Response:**
```json
{
  "email": "john.doe@mitaoe.ac.in",
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "role": "TEACHER"
}
```

**Error Responses:**
- `"Email not verified. Please check your email for verification link."`
- `"Account is not active. Please contact administrator."`
- `"Email or password is incorrect"`

---

### 4. Forgot Password (Public)

**Endpoint:** `POST /auth/forgot-password`

**Request:**
```json
{
  "email": "john.doe@mitaoe.ac.in"
}
```

**Response:**
```
"Password reset link has been sent to your email."
```

**What happens:**
1. Validates email exists
2. Generates password reset token (1h expiry)
3. Sends password reset email

---

### 5. Reset Password (Public)

**Endpoint:** `POST /auth/reset-password`

**Request:**
```json
{
  "token": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "newPassword": "newSecurePass456"
}
```

**Response:**
```
"Password reset successfully! You can now login with your new password."
```

**What happens:**
1. Validates token exists and not expired
2. Hashes and updates password
3. Clears reset token

---

## 📧 Email Templates

### Verification Email
```
Subject: SamaySetu - Email Verification

Dear Teacher,

Thank you for registering with SamaySetu Timetable Management System.

Please verify your email address by clicking the link below:
http://localhost:8083/auth/verify-email?token=YOUR_TOKEN

This link will expire in 24 hours.

If you did not register for this account, please ignore this email.

Best regards,
SamaySetu Team
```

### Password Reset Email
```
Subject: SamaySetu - Password Reset Request

Dear Teacher,

We received a request to reset your password.

Please click the link below to reset your password:
http://localhost:8083/auth/reset-password?token=YOUR_TOKEN

This link will expire in 1 hour.

If you did not request a password reset, please ignore this email.

Best regards,
SamaySetu Team
```

### Welcome Email
```
Subject: Welcome to SamaySetu!

Dear [Name],

Your email has been successfully verified!

You can now log in to SamaySetu Timetable Management System.

Best regards,
SamaySetu Team
```

---

## 🧪 Testing Flow

### Complete Registration & Login Flow

**Step 1: Register**
```bash
POST http://localhost:8083/auth/register
Content-Type: application/json

{
  "name": "Test Teacher",
  "employeeId": "TEST001",
  "email": "test@mitaoe.ac.in",
  "password": "test123",
  "phone": "1234567890"
}
```

**Step 2: Check Email**
- Open your email inbox
- Find verification email from SamaySetu
- Click the verification link OR copy the token

**Step 3: Verify Email**
```bash
GET http://localhost:8083/auth/verify-email?token=PASTE_TOKEN_HERE
```

**Step 4: Login**
```bash
POST http://localhost:8083/auth/login
Content-Type: application/json

{
  "email": "test@mitaoe.ac.in",
  "password": "test123"
}
```

**Step 5: Use JWT Token**
```bash
GET http://localhost:8083/api/teachers
Authorization: Bearer YOUR_JWT_TOKEN
```

---

### Password Reset Flow

**Step 1: Request Reset**
```bash
POST http://localhost:8083/auth/forgot-password
Content-Type: application/json

{
  "email": "test@mitaoe.ac.in"
}
```

**Step 2: Check Email**
- Open your email inbox
- Find password reset email
- Copy the token from the link

**Step 3: Reset Password**
```bash
POST http://localhost:8083/auth/reset-password
Content-Type: application/json

{
  "token": "PASTE_TOKEN_HERE",
  "newPassword": "newPassword123"
}
```

**Step 4: Login with New Password**
```bash
POST http://localhost:8083/auth/login
Content-Type: application/json

{
  "email": "test@mitaoe.ac.in",
  "password": "newPassword123"
}
```

---

## 🔒 Security Features

1. **Email Domain Validation**
   - Only `@mitaoe.ac.in` emails accepted
   - Validated at both DTO and service level

2. **Token Expiry**
   - Verification token: 24 hours
   - Password reset token: 1 hour

3. **Account Protection**
   - Cannot login without email verification
   - Inactive accounts cannot login

4. **Password Security**
   - BCrypt hashing with salt
   - Minimum 6 characters (can be increased)

5. **Token Security**
   - UUID-based tokens (random, unpredictable)
   - Single-use tokens (cleared after use)

---

## ⚠️ Important Notes

### For Development:
1. **Email Testing:** Use a real Gmail account for testing
2. **App Password:** Required if 2FA is enabled
3. **Firewall:** Ensure port 587 is not blocked

### For Production:
1. **Change Base URL:** Update `app.base-url` to your domain
2. **Use Environment Variables:** Don't commit email credentials
3. **HTTPS:** Use secure connections for email links
4. **Email Service:** Consider using SendGrid, AWS SES, or similar
5. **Rate Limiting:** Add rate limiting to prevent abuse

---

## 🐛 Troubleshooting

### Email Not Sending

**Issue:** Verification/reset emails not received

**Solutions:**
1. Check spam/junk folder
2. Verify Gmail App Password is correct
3. Ensure 2-Step Verification is enabled
4. Check console for error messages
5. Verify SMTP settings in application.properties

### Token Expired

**Issue:** "Verification token has expired"

**Solutions:**
1. Request new verification by registering again (will update token)
2. Or manually update token expiry in database:
```sql
UPDATE teachers 
SET verification_token_expiry = DATE_ADD(NOW(), INTERVAL 24 HOUR)
WHERE email = 'user@mitaoe.ac.in';
```

### Cannot Login After Verification

**Issue:** Still getting "Email not verified" error

**Solutions:**
1. Check database: `SELECT is_email_verified, is_active FROM teachers WHERE email = 'user@mitaoe.ac.in';`
2. Both should be `1` (true)
3. If not, manually update:
```sql
UPDATE teachers 
SET is_email_verified = 1, is_active = 1 
WHERE email = 'user@mitaoe.ac.in';
```

---

## 📊 Database Queries

### Check User Status
```sql
SELECT 
    name, 
    email, 
    is_email_verified, 
    is_active,
    verification_token_expiry,
    password_reset_token_expiry
FROM teachers 
WHERE email = 'user@mitaoe.ac.in';
```

### Manually Verify User
```sql
UPDATE teachers 
SET 
    is_email_verified = 1, 
    is_active = 1,
    verification_token = NULL,
    verification_token_expiry = NULL
WHERE email = 'user@mitaoe.ac.in';
```

### Reset Password Token
```sql
UPDATE teachers 
SET 
    password_reset_token = NULL,
    password_reset_token_expiry = NULL
WHERE email = 'user@mitaoe.ac.in';
```

---

## 🎨 Frontend Integration

### Registration Form
```javascript
const register = async (formData) => {
  const response = await fetch('http://localhost:8083/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(formData)
  });
  
  if (response.ok) {
    alert('Please check your email to verify your account');
  }
};
```

### Email Verification Page
```javascript
// Extract token from URL
const urlParams = new URLSearchParams(window.location.search);
const token = urlParams.get('token');

const verifyEmail = async (token) => {
  const response = await fetch(`http://localhost:8083/auth/verify-email?token=${token}`);
  
  if (response.ok) {
    alert('Email verified! Redirecting to login...');
    window.location.href = '/login';
  }
};
```

### Forgot Password Form
```javascript
const forgotPassword = async (email) => {
  const response = await fetch('http://localhost:8083/auth/forgot-password', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email })
  });
  
  if (response.ok) {
    alert('Password reset link sent to your email');
  }
};
```

---

## ✅ Checklist

Before going live, ensure:

- [ ] Gmail App Password configured
- [ ] Email sending tested
- [ ] Verification flow tested
- [ ] Password reset flow tested
- [ ] Login with unverified email blocked
- [ ] Token expiry working
- [ ] Base URL updated for production
- [ ] Email credentials in environment variables
- [ ] HTTPS enabled for production
- [ ] Rate limiting implemented
- [ ] Error handling tested
- [ ] Email templates customized

---

## 📞 Support

For issues or questions:
- Check console logs for detailed error messages
- Verify database schema updates
- Test email configuration with simple test
- Review SecurityConfig for endpoint permissions
