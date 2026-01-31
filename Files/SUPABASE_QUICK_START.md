# 🚀 Supabase Migration - Quick Start Guide

## ✅ Migration Package Ready!

All scripts and documentation prepared to migrate SamaySetu from MySQL to Supabase PostgreSQL.

### What's Included
- ✅ Complete migration script with all your data
- ✅ MySQL backup (3 years, 5 depts, 3 users, 22 time slots)
- ✅ 4 comprehensive documentation files
- ✅ Step-by-step execution guide
- ✅ Rollback plan

---

## 📚 Documentation Files

### 🎯 START HERE
**File**: `Files/MIGRATION_EXECUTION_STEPS.md`
- Simple numbered steps (1, 2, 3...)
- Time estimates for each phase
- Success criteria
- Quick troubleshooting

### 📖 Detailed Guide
**File**: `Files/SUPABASE_COMPLETE_MIGRATION_GUIDE.md`
- Complete explanations
- Configuration details
- Entity class updates
- Troubleshooting section
- Rollback instructions

### ⚡ Quick Reference
**File**: `Files/ENTITY_UPDATE_CHECKLIST.md`
- Table name mappings
- Foreign key changes
- Files to update
- Common errors

### 📊 Complete Overview
**File**: `Files/SUPABASE_MIGRATION_SUMMARY.md`
- Migration summary
- Benefits of Supabase
- Pre-migration checklist
- File structure

---

## ⚡ Quick Steps (40 minutes total)

### 1. Setup Supabase (10 min)
```
1. Go to https://supabase.com
2. Create new project: "samaysetu"
3. Save database password
4. Note connection details
```

### 2. Run Migration Script (5 min)
```
1. Open Supabase SQL Editor
2. Copy Scripts/supabase_complete_migration.sql
3. Paste and execute
4. Verify data counts (3 years, 5 depts, 3 users, etc.)
```

### 3. Update Backend (15 min)
```
Update 6 files:
1. pom.xml - Add PostgreSQL driver
2. application.properties - Update connection
3. TeacherEntity.java - Change to "users"
4. ClassRoom.java - Change to "classrooms"
5. TeacherAvailability.java - Change table and FK
6. TimetableEntry.java - Update foreign keys
```

### 4. Build & Test (10 min)
```bash
mvn clean install
mvn spring-boot:run
# Test login and verify data
```

---

## 📊 Database Changes

### Table Renames (Clean Schema)
| Old (MySQL) | New (Supabase) |
|-------------|----------------|
| teachers | users |
| rooms | classrooms |
| teacher_availability | user_availability |

### Foreign Key Updates
| Old Column | New Column |
|------------|------------|
| teacher_id | user_id |
| room_id | classroom_id |

### Data Type Conversions
```
BIT(1)           → BOOLEAN
datetime(6)      → TIMESTAMP
AUTO_INCREMENT   → BIGSERIAL
tinyint(1)       → BOOLEAN
```

---

## 📝 Files to Update

### Configuration (2 files)
1. **pom.xml**
   ```xml
   <!-- Remove MySQL, Add PostgreSQL -->
   <dependency>
       <groupId>org.postgresql</groupId>
       <artifactId>postgresql</artifactId>
       <scope>runtime</scope>
   </dependency>
   ```

2. **application.properties**
   ```properties
   spring.datasource.url=jdbc:postgresql://db.xxxxx.supabase.co:5432/postgres
   spring.datasource.username=postgres
   spring.datasource.password=YOUR_PASSWORD
   spring.datasource.driver-class-name=org.postgresql.Driver
   spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
   ```

### Entity Files (4 files)
1. **TeacherEntity.java**: `@Table(name = "users")`
2. **ClassRoom.java**: `@Table(name = "classrooms")`
3. **TeacherAvailability.java**: `@Table(name = "user_availability")` + FK
4. **TimetableEntry.java**: Update foreign keys

---

## ✅ Success Criteria

Migration successful when:
- ✅ Backend starts without errors
- ✅ Login works with existing credentials
- ✅ All data visible in frontend
- ✅ Can create new records
- ✅ Supabase dashboard shows all data

---

## 🔒 Safety & Backup

- ✅ MySQL backup: `mysql_backups/samaysetu_backup.sql`
- ✅ Rollback plan ready
- ✅ Risk level: Low

---

## 🆘 Common Issues

### Connection Refused
```properties
# Check connection string and password
spring.datasource.url=jdbc:postgresql://db.xxxxx.supabase.co:5432/postgres
```

### Table "teachers" Not Found
```java
// Update TeacherEntity.java
@Table(name = "users")  // Changed from "teachers"
```

### Column "teacher_id" Not Found
```java
// Update foreign keys
@JoinColumn(name = "user_id")  // Changed from "teacher_id"
```

---

## 🎯 Benefits of Supabase

✅ **Free Tier**: 500MB database, 2GB bandwidth  
✅ **Auto Backups**: Daily backups included  
✅ **Real-time**: Built-in real-time subscriptions  
✅ **Dashboard**: Beautiful admin interface  
✅ **API**: Auto-generated REST API  
✅ **SSL**: Free SSL certificates  
✅ **Scalable**: Easy to upgrade  
✅ **PostgreSQL**: Better performance & features

---

## 📞 Need Help?

### For Step-by-Step Instructions
**Read**: `Files/MIGRATION_EXECUTION_STEPS.md`

### For Detailed Explanations
**Read**: `Files/SUPABASE_COMPLETE_MIGRATION_GUIDE.md`

### For Quick Reference
**Read**: `Files/ENTITY_UPDATE_CHECKLIST.md`

### For Complete Overview
**Read**: `Files/SUPABASE_MIGRATION_SUMMARY.md`

---

## 🏁 Ready to Start?

**Open**: `Files/MIGRATION_EXECUTION_STEPS.md`

Follow the 4 phases:
1. Supabase Setup (10 min)
2. Data Migration (5 min)
3. Backend Updates (15 min)
4. Build & Test (10 min)

---

**Status**: ✅ Ready for execution  
**Time Required**: ~40 minutes  
**Difficulty**: Medium  
**Risk**: Low (backup available)

---

**Quick Start Guide for MIT Academy of Engineering**

© 2024 MIT Academy of Engineering - SamaySetu Development Team