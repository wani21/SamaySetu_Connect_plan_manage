# 🚀 Quick Start Guide - SamaySetu

## Prerequisites

### Required Software
- **Node.js**: v18+ ([Download](https://nodejs.org/))
- **Java**: JDK 17+ ([Download](https://www.oracle.com/java/technologies/downloads/))
- **MySQL**: 8.0+ ([Download](https://dev.mysql.com/downloads/))
- **Git**: Latest version

---

## 🗄️ Database Setup

### 1. Create Database
```sql
CREATE DATABASE college_timetable;
USE college_timetable;
```

### 2. Run Schema Script
```bash
mysql -u root -p college_timetable < database.txt
```

### 3. Create Admin User
```sql
-- Run the create_admin_user.sql script
mysql -u root -p college_timetable < create_admin_user.sql
```

**Default Admin Credentials:**
- Email: `admin@mitaoe.ac.in`
- Password: `admin123`

---

## 🔧 Backend Setup (Spring Boot)

### 1. Navigate to Backend Directory
```bash
cd Backend
```

### 2. Configure Database
Edit `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/college_timetable
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD

# Email Configuration (for verification)
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
```

### 3. Build and Run
```bash
# Using Maven Wrapper (Windows)
mvnw.cmd spring-boot:run

# Using Maven Wrapper (Mac/Linux)
./mvnw spring-boot:run

# Or using Maven directly
mvn spring-boot:run
```

**Backend will start on:** `http://localhost:8083`

---

## 🎨 Frontend Setup (React)

### 1. Navigate to Frontend Directory
```bash
cd Frontend
```

### 2. Install Dependencies
```bash
npm install
```

### 3. Start Development Server
```bash
npm run dev
```

**Frontend will start on:** `http://localhost:5173`

---

## 🎯 Access the Application

### Open Browser
Navigate to: `http://localhost:5173`

### Login Options

#### **Admin Login**
- Email: `admin@mitaoe.ac.in`
- Password: `admin123`
- Access: Full system control

#### **Teacher Login** (Create via Register)
- Email: `yourname@mitaoe.ac.in`
- Password: Your choice
- Access: Teacher dashboard

---

## 📱 Features to Explore

### **As Admin**
1. **Dashboard**: View system statistics
2. **Departments**: Add/View departments (CS, IT, etc.)
3. **Teachers**: Manage teacher records
4. **Courses**: Add courses with credits and hours
5. **Rooms**: Manage classrooms and labs
6. **Academic Years**: Create and manage academic years
7. **Divisions**: Add student divisions (SE-A, TE-B, etc.)
8. **Time Slots**: Configure class periods

### **As Teacher**
1. **Dashboard**: View personal statistics
2. **My Timetable**: View teaching schedule
3. **Availability**: Set available hours
4. **Profile**: Update personal information

---

## 🔍 Testing the System

### 1. Register a New Teacher
```
1. Go to Register page
2. Fill form with @mitaoe.ac.in email
3. Check email for verification link
4. Verify email
5. Login with credentials
```

### 2. Create Master Data (As Admin)
```
1. Login as admin
2. Create Department (e.g., Computer Science)
3. Create Academic Year (e.g., 2024-25)
4. Create Courses (e.g., Data Structures)
5. Create Rooms (e.g., Room 101)
6. Create Divisions (e.g., SE-A)
7. Create Time Slots (e.g., 9:00-10:00)
```

### 3. Test Features
```
✅ Form validation (try empty fields)
✅ Email validation (try non-college email)
✅ Error messages (wrong password)
✅ User profile dropdown
✅ Responsive design (resize browser)
✅ Navigation between pages
✅ Modal forms
✅ Toast notifications
```

---

## 🐛 Troubleshooting

### Backend Not Starting
```bash
# Check if port 8083 is in use
netstat -ano | findstr :8083

# Kill process if needed (Windows)
taskkill /PID <PID> /F

# Check MySQL connection
mysql -u root -p
```

### Frontend Not Starting
```bash
# Clear node_modules and reinstall
rm -rf node_modules package-lock.json
npm install

# Check if port 5173 is in use
netstat -ano | findstr :5173
```

### Database Connection Error
```
1. Verify MySQL is running
2. Check credentials in application.properties
3. Ensure database exists
4. Check firewall settings
```

### Email Verification Not Working
```
1. Configure Gmail App Password
2. Enable 2-Factor Authentication
3. Generate App Password
4. Update application.properties
```

---

## 📊 Default Data

### After Setup, You'll Have:
- ✅ 1 Admin user
- ✅ Empty departments (add via UI)
- ✅ Empty teachers (add via UI)
- ✅ Empty courses (add via UI)
- ✅ Empty rooms (add via UI)
- ✅ Empty academic years (add via UI)
- ✅ Empty divisions (add via UI)
- ✅ Empty time slots (add via UI)

---

## 🎨 UI Preview

### Login Page
- College email validation
- Password visibility toggle
- Forgot password link
- Register link

### Admin Dashboard
- Statistics cards
- Quick actions
- Recent activity
- Navigation sidebar

### CRUD Pages
- Grid layout with cards
- Add button with modal
- Edit/Delete actions
- Form validation
- Toast notifications

---

## 📝 Sample Data to Add

### Department
```
Name: Computer Science
Code: CS
HOD: Dr. John Smith
```

### Academic Year
```
Year Name: 2024-25
Start Date: 2024-07-01
End Date: 2025-06-30
Is Current: Yes
```

### Course
```
Name: Data Structures
Code: CS301
Type: Theory
Credits: 4
Hours/Week: 4
Semester: 3
Department: Computer Science
```

### Room
```
Name: Room 101
Room Number: 101
Type: Classroom
Capacity: 60
Department: Computer Science
Facilities: Projector, AC
```

### Division
```
Name: A
Year: SY (Second Year)
Branch: Computer Science
Department: Computer Science
Academic Year: 2024-25
Total Students: 60
```

### Time Slot
```
Slot Name: Period 1
Start Time: 09:00
End Time: 10:00
Is Break: No
```

---

## 🔐 Security Notes

### Important
- ✅ Change default admin password immediately
- ✅ Use strong passwords for all accounts
- ✅ Keep JWT secret secure
- ✅ Use HTTPS in production
- ✅ Regular database backups

### Email Configuration
- Use App Passwords, not regular passwords
- Enable 2FA on email account
- Don't commit credentials to Git

---

## 📚 Additional Resources

### Documentation
- `PROJECT_DESCRIPTION.md` - Complete project overview
- `DATABASE_SCHEMA_DETAILED.md` - Database structure
- `PHASE_1_COMPLETE.md` - Implementation status
- `FIXES_APPLIED.md` - Recent fixes
- `ERROR_MESSAGES_GUIDE.md` - Error handling

### Support
- Check console for errors
- Review browser network tab
- Check backend logs
- Verify database connections

---

## ✅ Verification Checklist

Before demonstration, verify:
- [ ] MySQL is running
- [ ] Database is created and populated
- [ ] Backend is running on port 8083
- [ ] Frontend is running on port 5173
- [ ] Admin login works
- [ ] Can create departments
- [ ] Can create teachers
- [ ] Can create courses
- [ ] Can create rooms
- [ ] Can create academic years
- [ ] Can create divisions
- [ ] Can create time slots
- [ ] Toast notifications appear
- [ ] Forms validate correctly
- [ ] Responsive design works

---

## 🎉 You're Ready!

The system is now running and ready for:
- ✅ Demonstration
- ✅ Testing
- ✅ Development
- ✅ Phase 2 implementation

**Enjoy using SamaySetu!** 🚀

---

**Need Help?**
- Check documentation files
- Review error messages
- Verify all services are running
- Check browser console for errors

**Happy Coding!** 💻

