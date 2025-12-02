# ⚡ Quick Email Setup - 5 Minutes

## Step 1: Get Gmail App Password (2 minutes)

1. Go to: https://myaccount.google.com/apppasswords
2. Sign in with your Gmail account
3. If you don't see "App passwords":
   - Go to Security → Enable 2-Step Verification first
   - Then return to App passwords
4. Select:
   - **App:** Mail
   - **Device:** Other (Custom name) → Type "SamaySetu"
5. Click **Generate**
6. **Copy the 16-character password** (e.g., `abcd efgh ijkl mnop`)

---

## Step 2: Update application.properties (1 minute)

Open: `Backend/src/main/resources/application.properties`

Replace these lines:
```properties
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
```

With your actual values:
```properties
spring.mail.username=yourname@mitaoe.ac.in
spring.mail.password=abcdefghijklmnop
```

**Note:** Remove spaces from the app password!

---

## Step 3: Restart Backend (1 minute)

```bash
cd Backend
mvnw.cmd spring-boot:run
```

---

## Step 4: Test Registration (1 minute)

**Postman Request:**
```
POST http://localhost:8083/auth/register
Content-Type: application/json

{
  "name": "Test Teacher",
  "employeeId": "TEST001",
  "email": "yourname@mitaoe.ac.in",
  "password": "test123",
  "phone": "1234567890"
}
```

**Expected Response:**
```
"Registration successful! Please check your college email to verify your account."
```

---

## Step 5: Verify Email (30 seconds)

1. Check your email inbox
2. Open the verification email
3. Click the link OR copy the token
4. Visit: `http://localhost:8083/auth/verify-email?token=YOUR_TOKEN`

**Expected Response:**
```
"Email verified successfully! You can now login."
```

---

## ✅ Done!

Now you can login:
```
POST http://localhost:8083/auth/login

{
  "email": "yourname@mitaoe.ac.in",
  "password": "test123"
}
```

---

## 🚨 Troubleshooting

### Email not received?
1. Check spam folder
2. Verify app password has no spaces
3. Check console for errors
4. Try with a different Gmail account

### "Invalid email" error?
- Email must end with `@mitaoe.ac.in`
- Check for typos

### Still not working?
- Check `EMAIL_VERIFICATION_SETUP.md` for detailed troubleshooting
- Verify MySQL is running
- Check backend console for error messages

---

## 🔐 Security Note

**Never commit your email password to Git!**

For production, use environment variables:
```bash
export MAIL_USERNAME=your-email@mitaoe.ac.in
export MAIL_PASSWORD=your-app-password
```

Then in application.properties:
```properties
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}
```
