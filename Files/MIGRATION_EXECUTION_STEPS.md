# Supabase Migration - Step-by-Step Execution Guide

## 🎯 Goal
Migrate SamaySetu from MySQL to Supabase PostgreSQL without losing any data.

## ⏱️ Estimated Time
30-45 minutes

## 📋 Prerequisites
- [x] MySQL backup created: `mysql_backups/samaysetu_backup.sql`
- [ ] Supabase account (free)
- [ ] Internet connection
- [ ] Code editor (VS Code)

---

## 🚀 Phase 1: Supabase Setup (10 minutes)

### Step 1.1: Create Supabase Project
1. Go to https://supabase.com
2. Click **"Start your project"** or **"New Project"**
3. Sign in with GitHub/Google/Email
4. Click **"New Project"**
5. Fill in:
   - **Name**: `samaysetu`
   - **Database Password**: Create a strong password (SAVE THIS!)
   - **Region**: Choose closest to India (e.g., Mumbai/Singapore)
   - **Pricing Plan**: Free
6. Click **"Create new project"**
7. Wait 2-3 minutes for setup

### Step 1.2: Get Connection Details
1. In Supabase dashboard, click **Settings** (gear icon)
2. Click **Database** in left sidebar
3. Scroll to **Connection String** section
4. Note down:
   - **Host**: `db.xxxxxxxxxxxxx.supabase.co`
   - **Database**: `postgres`
   - **Port**: `5432`
   - **User**: `postgres`
   - **Password**: (the one you created)

---

## 📊 Phase 2: Data Migration (5 minutes)

### Step 2.1: Run Migration Script
1. In Supabase dashboard, click **SQL Editor** (left sidebar)
2. Click **"New query"** button
3. Open file: `Scripts/supabase_complete_migration.sql`
4. Copy ALL content (Ctrl+A, Ctrl+C)
5. Paste into Supabase SQL Editor (Ctrl+V)
6. Click **"Run"** button (or press Ctrl+Enter)
7. Wait for execution (30-60 seconds)

### Step 2.2: Verify Migration
Check the results at the bottom of SQL Editor:

```
✅ Expected Results:
academic_years: 3
departments: 5
users: 3
divisions: 5
batches: 6
courses: 1
classrooms: 4
time_slots: 22
students: 0
user_availability: 0
teacher_courses: 0
timetable_entries: 0
```

If numbers match, migration is successful! ✅

### Step 2.3: Visual Verification
1. Click **Table Editor** in left sidebar
2. Click on each table to see data:
   - `academic_years` - should show 3 years
   - `departments` - should show 5 departments
   - `users` - should show 3 users (admin + 2 teachers)
   - `divisions` - should show 5 divisions
   - `batches` - should show 6 batches
   - `classrooms` - should show 4 rooms
   - `time_slots` - should show 22 slots

---

## 💻 Phase 3: Backend Updates (15 minutes)

### Step 3.1: Update pom.xml
1. Open `Backend/pom.xml`
2. Find MySQL dependency (around line 40-45):
   ```xml
   <dependency>
       <groupId>com.mysql</groupId>
       <artifactId>mysql-connector-j</artifactId>
       <scope>runtime</scope>
   </dependency>
   ```
3. Comment it out or delete it
4. Add PostgreSQL dependency:
   ```xml
   <dependency>
       <groupId>org.postgresql</groupId>
       <artifactId>postgresql</artifactId>
       <scope>runtime</scope>
   </dependency>
   ```
5. Save file

### Step 3.2: Update application.properties
1. Open `Backend/src/main/resources/application.properties`
2. Find database configuration section
3. Replace with:
   ```properties
   # Database Configuration - Supabase PostgreSQL
   spring.datasource.url=jdbc:postgresql://db.xxxxxxxxxxxxx.supabase.co:5432/postgres
   spring.datasource.username=postgres
   spring.datasource.password=YOUR_PASSWORD_HERE
   spring.datasource.driver-class-name=org.postgresql.Driver
   
   # JPA Configuration
   spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
   spring.jpa.hibernate.ddl-auto=none
   spring.jpa.show-sql=true
   spring.jpa.properties.hibernate.format_sql=true
   ```
4. Replace:
   - `xxxxxxxxxxxxx` with your Supabase project reference
   - `YOUR_PASSWORD_HERE` with your database password
5. Save file

### Step 3.3: Update Entity Files

#### File 1: TeacherEntity.java
1. Open `Backend/src/main/java/com/College/timetable/Entity/TeacherEntity.java`
2. Find: `@Table(name = "teachers")`
3. Change to: `@Table(name = "users")`
4. Save file

#### File 2: ClassRoom.java
1. Open `Backend/src/main/java/com/College/timetable/Entity/ClassRoom.java`
2. Find: `@Table(name = "rooms")`
3. Change to: `@Table(name = "classrooms")`
4. Save file

#### File 3: TeacherAvailability.java
1. Open `Backend/src/main/java/com/College/timetable/Entity/TeacherAvailability.java`
2. Find: `@Table(name = "teacher_availability")`
3. Change to: `@Table(name = "user_availability")`
4. Find: `@JoinColumn(name = "teacher_id")`
5. Change to: `@JoinColumn(name = "user_id")`
6. Save file

#### File 4: TimetableEntry.java
1. Open `Backend/src/main/java/com/College/timetable/Entity/TimetableEntry.java`
2. Find all occurrences of:
   - `@JoinColumn(name = "teacher_id")` → Change to `"user_id"`
   - `@JoinColumn(name = "room_id")` → Change to `"classroom_id"`
3. Save file

---

## 🔨 Phase 4: Build & Test (10 minutes)

### Step 4.1: Clean Build
1. Open terminal in project root
2. Navigate to Backend:
   ```bash
   cd Backend
   ```
3. Clean and build:
   ```bash
   mvn clean install
   ```
4. Wait for build to complete
5. Check for errors - should see **BUILD SUCCESS**

### Step 4.2: Start Application
1. Run application:
   ```bash
   mvn spring-boot:run
   ```
2. Watch logs for:
   - ✅ `HikariPool-1 - Start completed`
   - ✅ `Started samaysetuApplication`
   - ❌ No errors about tables not found
   - ❌ No errors about columns not found

### Step 4.3: Test Login
1. Open Postman or browser
2. Test login endpoint:
   ```
   POST http://localhost:8080/api/auth/login
   Body:
   {
     "email": "admin@mitaoe.ac.in",
     "password": "admin123"
   }
   ```
3. Should receive JWT token ✅

### Step 4.4: Test Frontend
1. Open new terminal
2. Navigate to Frontend:
   ```bash
   cd Frontend
   ```
3. Start frontend:
   ```bash
   npm run dev
   ```
4. Open browser: `http://localhost:5173`
5. Login with admin credentials
6. Check:
   - ✅ Dashboard loads
   - ✅ Academic Structure shows data
   - ✅ Departments visible
   - ✅ Divisions visible
   - ✅ Time slots visible

---

## ✅ Phase 5: Final Verification

### Checklist
- [ ] Supabase project created
- [ ] Migration script executed successfully
- [ ] All tables have correct data
- [ ] `pom.xml` updated with PostgreSQL driver
- [ ] `application.properties` updated with Supabase connection
- [ ] All 4 entity files updated
- [ ] Backend builds without errors
- [ ] Backend starts without errors
- [ ] Login works
- [ ] Frontend connects to backend
- [ ] All data visible in frontend

---

## 🎉 Success Criteria

You've successfully migrated when:
1. ✅ Backend starts without errors
2. ✅ You can login with existing credentials
3. ✅ All departments, divisions, and data are visible
4. ✅ You can create new records (test with a new time slot)
5. ✅ Supabase dashboard shows all data

---

## 🆘 Troubleshooting

### Problem: "Table 'teachers' doesn't exist"
**Solution**: You forgot to update `TeacherEntity.java`
- Change `@Table(name = "teachers")` to `@Table(name = "users")`
- Rebuild: `mvn clean install`

### Problem: "Connection refused"
**Solution**: Check `application.properties`
- Verify Supabase URL is correct
- Verify password is correct
- Check if your IP is allowed (Supabase allows all by default)

### Problem: "Column 'teacher_id' doesn't exist"
**Solution**: Update foreign key references
- In `TeacherAvailability.java`: Change to `user_id`
- In `TimetableEntry.java`: Change to `user_id`
- Rebuild: `mvn clean install`

### Problem: "Driver class not found"
**Solution**: PostgreSQL driver not loaded
- Check `pom.xml` has PostgreSQL dependency
- Run `mvn clean install` again
- Restart IDE if needed

### Problem: Build fails with dependency errors
**Solution**: 
```bash
mvn clean install -U
```
The `-U` flag forces update of dependencies

---

## 🔄 Rollback (If Needed)

If something goes wrong:

1. **Stop the application**
2. **Restore MySQL**:
   ```bash
   mysql -u root -p samaysetu < mysql_backups/samaysetu_backup.sql
   ```
3. **Revert application.properties** to MySQL config
4. **Revert pom.xml** to MySQL driver
5. **Revert entity files** to old table names
6. **Rebuild**: `mvn clean install`

---

## 📝 Post-Migration Tasks

After successful migration:

1. **Update README.md** with new database info
2. **Update deployment docs** with Supabase details
3. **Share Supabase credentials** with team (securely)
4. **Set up Supabase backups** (automatic in free tier)
5. **Monitor performance** for first few days
6. **Delete old MySQL database** (after 1 week of stable operation)

---

## 📞 Need Help?

If you get stuck:
1. Check `Files/SUPABASE_COMPLETE_MIGRATION_GUIDE.md` for detailed explanations
2. Check `Files/ENTITY_UPDATE_CHECKLIST.md` for entity changes
3. Check Supabase logs in dashboard
4. Check Spring Boot logs in terminal

---

## 🎯 Quick Reference

### Supabase Dashboard URLs
- **Project Dashboard**: https://supabase.com/dashboard/project/YOUR_PROJECT_ID
- **Table Editor**: https://supabase.com/dashboard/project/YOUR_PROJECT_ID/editor
- **SQL Editor**: https://supabase.com/dashboard/project/YOUR_PROJECT_ID/sql

### Important Files
- Migration Script: `Scripts/supabase_complete_migration.sql`
- MySQL Backup: `mysql_backups/samaysetu_backup.sql`
- Config File: `Backend/src/main/resources/application.properties`
- Dependencies: `Backend/pom.xml`

### Entity Files to Update
1. `Backend/src/main/java/com/College/timetable/Entity/TeacherEntity.java`
2. `Backend/src/main/java/com/College/timetable/Entity/ClassRoom.java`
3. `Backend/src/main/java/com/College/timetable/Entity/TeacherAvailability.java`
4. `Backend/src/main/java/com/College/timetable/Entity/TimetableEntry.java`

---

**Good luck with your migration! 🚀**

**Estimated Total Time**: 30-45 minutes  
**Difficulty**: Medium  
**Success Rate**: High (with proper steps)
