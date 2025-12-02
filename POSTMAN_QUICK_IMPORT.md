# ⚡ Postman Quick Import Guide (2 Minutes)

## 📦 Files to Import

1. **SamaySetu_Postman_Collection.json** - API endpoints
2. **SamaySetu_Postman_Environment.json** - Variables

---

## 🚀 Import Steps

### Step 1: Import Collection (30 seconds)

1. Open Postman
2. Click **Import** button (top left corner)
3. Drag `SamaySetu_Postman_Collection.json` into the window
4. Click **Import**
5. ✅ Collection appears in left sidebar

### Step 2: Import Environment (30 seconds)

1. Click **Import** again
2. Drag `SamaySetu_Postman_Environment.json` into the window
3. Click **Import**
4. ✅ Environment appears in environment dropdown

### Step 3: Select Environment (10 seconds)

1. Click environment dropdown (top right)
2. Select **"SamaySetu Local Environment"**
3. ✅ Environment active (shows in top right)

---

## 🧪 Quick Test (1 minute)

### Test 1: Check Backend Running

1. Open collection: **SamaySetu Timetable API**
2. Open folder: **Authentication**
3. Click: **4. Login**
4. Click **Send**
5. Should get error (no user yet) - Backend is working!

### Test 2: Register & Login

1. Click: **2. Register Teacher**
2. Update email in body to your college email
3. Click **Send**
4. Check your email for verification link
5. Copy token from email
6. Click: **3. Verify Email**
7. Paste token in `verification_token` variable
8. Click **Send**
9. Click: **4. Login**
10. Click **Send**
11. ✅ JWT token auto-saved!

---

## 📁 What You Get

### 13 API Endpoints:

**Authentication (6):**
- Generate Password Hash
- Register Teacher
- Verify Email
- Login
- Forgot Password
- Reset Password

**Teachers (1):**
- Create Teacher

**Admin Endpoints (6):**
- Create/Get Departments
- Create Course
- Create Room
- Create Academic Year
- Create Division

---

## 🎯 Quick Reference

### Variables Available:
- `{{base_url}}` - http://localhost:8083
- `{{jwt_token}}` - Auto-set after login
- `{{verification_token}}` - Copy from email
- `{{reset_token}}` - Copy from reset email

### Authorization:
- Public: `/auth/**`
- Teacher: `/api/teachers`
- Admin: `/admin/**`

---

## ✅ Checklist

Before testing:
- [ ] Backend running (`mvnw.cmd spring-boot:run`)
- [ ] MySQL running
- [ ] Email configured
- [ ] Collection imported
- [ ] Environment imported
- [ ] Environment selected

---

## 🐛 Common Issues

### "Could not send request"
- ✅ Check backend is running on port 8083
- ✅ Try: `http://localhost:8083/auth/login`

### "403 Forbidden"
- ✅ Login first to get JWT token
- ✅ Check email is verified
- ✅ Use correct role (TEACHER/ADMIN)

### "Email not verified"
- ✅ Check email inbox (and spam)
- ✅ Run "Verify Email" request
- ✅ Then login

---

## 📚 Full Documentation

For detailed guide, see: **POSTMAN_COLLECTION_GUIDE.md**

---

## 🎉 You're Done!

Collection imported and ready to use!

**Next Steps:**
1. Start backend
2. Run "Register Teacher"
3. Verify email
4. Login
5. Test protected endpoints

Happy Testing! 🚀
