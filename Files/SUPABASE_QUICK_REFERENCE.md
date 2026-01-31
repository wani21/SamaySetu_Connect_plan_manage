# Supabase Quick Reference Card

## 🚀 Quick Start (3 Steps)

### 1. Get Supabase Credentials
```
Supabase Dashboard → Settings → Database → Connection String
```

### 2. Update application.properties
```properties
spring.datasource.url=jdbc:postgresql://YOUR_HOST:6543/postgres
spring.datasource.username=postgres.YOUR_PROJECT_REF
spring.datasource.password=YOUR_PASSWORD
```

### 3. Build & Run
```bash
mvn clean install
mvn spring-boot:run
```

---

## 📝 What Changed

### Tables Renamed
```
teachers              → users
rooms                 → classrooms
teacher_availability  → user_availability
```

### Foreign Keys Updated
```
teacher_id  → user_id
room_id     → classroom_id
```

### Files Modified (6)
```
✅ pom.xml
✅ application.properties
✅ TeacherEntity.java
✅ ClassRoom.java
✅ TeacherAvailability.java
✅ TimetableEntry.java
```

---

## 🔧 Connection Examples

### Development (Direct)
```properties
spring.datasource.url=jdbc:postgresql://db.xxxxx.supabase.co:5432/postgres
```

### Production (Pooler)
```properties
spring.datasource.url=jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:6543/postgres
```

### With SSL
```properties
spring.datasource.url=jdbc:postgresql://...?sslmode=require
```

---

## 🔒 RLS (Row Level Security)

### Enable RLS
```sql
-- Run in Supabase SQL Editor
-- File: Scripts/supabase_rls_policies.sql
```

### RLS Policies
- **Admins**: Full access
- **Teachers**: Read + manage own data
- **All Users**: Read academic data
- **Service Role**: Bypass RLS (Spring Boot)

---

## 🧪 Test Commands

### Build
```bash
mvn clean install
```

### Run
```bash
mvn spring-boot:run
```

### Test Login
```bash
curl -X POST http://localhost:8083/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@mitaoe.ac.in","password":"admin123"}'
```

---

## 📊 Data Migrated

```
✅ 3 Academic Years
✅ 5 Departments
✅ 3 Users (1 Admin, 2 Teachers)
✅ 5 Divisions
✅ 6 Batches
✅ 1 Course
✅ 4 Classrooms
✅ 22 Time Slots
```

---

## 🆘 Quick Fixes

### Connection Refused
```properties
# Check host, username, password
spring.datasource.url=jdbc:postgresql://CORRECT_HOST:6543/postgres
spring.datasource.username=postgres.YOUR_PROJECT_REF
```

### Table Not Found
```
Already fixed! Tables renamed in entity classes.
```

### Authentication Failed
```properties
# Username must include project reference
spring.datasource.username=postgres.abcdefghijklmnop
```

### SSL Error
```properties
spring.datasource.url=jdbc:postgresql://...?sslmode=require
```

---

## 📚 Documentation

| File | Purpose |
|------|---------|
| MIGRATION_EXECUTION_STEPS.md | Step-by-step guide |
| SUPABASE_COMPLETE_MIGRATION_GUIDE.md | Detailed guide |
| ENTITY_UPDATE_CHECKLIST.md | Quick reference |
| SUPABASE_CONNECTION_SETUP.md | Connection guide |
| SUPABASE_IMPLEMENTATION_COMPLETE.md | Implementation status |

---

## ✅ Checklist

- [ ] Get Supabase credentials
- [ ] Update application.properties
- [ ] Build project
- [ ] Start application
- [ ] Test login
- [ ] Verify data
- [ ] Enable RLS (optional)

---

## 🎯 Status

**Implementation**: ✅ COMPLETE  
**Migration Script**: ✅ RUN  
**RLS Script**: ⏳ READY TO RUN  
**Backend**: ✅ UPDATED  
**Frontend**: ✅ NO CHANGES NEEDED

---

**Quick Reference for MIT Academy of Engineering - SamaySetu**
