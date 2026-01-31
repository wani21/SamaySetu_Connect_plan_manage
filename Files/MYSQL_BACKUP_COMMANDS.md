# 📦 MySQL Backup Commands

Quick reference for backing up your MySQL database before Supabase migration.

## 🔑 Your Database Credentials

Based on your `application.properties`:
- **Database Name**: `samaysetu`
- **Username**: `root`
- **Password**: `root`
- **Host**: `localhost`
- **Port**: `3306`

## 🚀 Quick Backup Commands

### Option 1: Using PowerShell Script (Recommended)
```powershell
# Run the automated backup script
.\Scripts\backup_mysql_database.ps1
```

### Option 2: Using Batch File
```cmd
# Run the batch script
Scripts\backup_mysql_database.bat
```

### Option 3: Manual Commands

#### Full Database Backup
```bash
mysqldump -u root -p samaysetu > mysql_backups/samaysetu_full_backup.sql
# Enter password: root
```

#### Data Only (No CREATE statements)
```bash
mysqldump -u root -p --no-create-info samaysetu > mysql_backups/samaysetu_data_only.sql
```

#### Schema Only (No data)
```bash
mysqldump -u root -p --no-data samaysetu > mysql_backups/samaysetu_schema_only.sql
```

#### Specific Tables
```bash
mysqldump -u root -p samaysetu teachers academic_years departments courses divisions batches time_slots class_rooms > mysql_backups/samaysetu_tables.sql
```

## 📊 Export to CSV (For Supabase Import)

### Method 1: Using MySQL Workbench
```
1. Open MySQL Workbench
2. Connect to your database
3. Right-click on table → "Table Data Export Wizard"
4. Select CSV format
5. Choose output location
6. Export
```

### Method 2: Using SQL Commands
```sql
-- Connect to MySQL
mysql -u root -p samaysetu

-- Find secure file directory
SHOW VARIABLES LIKE 'secure_file_priv';

-- Export each table (replace path with your secure_file_priv path)
SELECT * FROM teachers 
INTO OUTFILE 'C:/ProgramData/MySQL/MySQL Server 8.0/Uploads/teachers.csv'
FIELDS TERMINATED BY ',' 
ENCLOSED BY '"' 
LINES TERMINATED BY '\n';

SELECT * FROM academic_years 
INTO OUTFILE 'C:/ProgramData/MySQL/MySQL Server 8.0/Uploads/academic_years.csv'
FIELDS TERMINATED BY ',' 
ENCLOSED BY '"' 
LINES TERMINATED BY '\n';

-- Repeat for all tables
```

### Method 3: Using mysqldump with CSV
```bash
# Export all tables as CSV
mysql -u root -p samaysetu -e "SELECT * FROM teachers" | sed 's/\t/,/g' > mysql_backups/teachers.csv
mysql -u root -p samaysetu -e "SELECT * FROM academic_years" | sed 's/\t/,/g' > mysql_backups/academic_years.csv
mysql -u root -p samaysetu -e "SELECT * FROM departments" | sed 's/\t/,/g' > mysql_backups/departments.csv
mysql -u root -p samaysetu -e "SELECT * FROM courses" | sed 's/\t/,/g' > mysql_backups/courses.csv
mysql -u root -p samaysetu -e "SELECT * FROM divisions" | sed 's/\t/,/g' > mysql_backups/divisions.csv
mysql -u root -p samaysetu -e "SELECT * FROM batches" | sed 's/\t/,/g' > mysql_backups/batches.csv
mysql -u root -p samaysetu -e "SELECT * FROM time_slots" | sed 's/\t/,/g' > mysql_backups/time_slots.csv
mysql -u root -p samaysetu -e "SELECT * FROM class_rooms" | sed 's/\t/,/g' > mysql_backups/class_rooms.csv
```

## 🔍 Verify Backup

### Check Backup File
```bash
# Check if file exists and size
dir mysql_backups

# View first few lines
Get-Content mysql_backups/samaysetu_full_backup.sql -Head 20
```

### Test Restore (Optional)
```bash
# Create test database
mysql -u root -p -e "CREATE DATABASE samaysetu_test;"

# Restore backup
mysql -u root -p samaysetu_test < mysql_backups/samaysetu_full_backup.sql

# Verify
mysql -u root -p samaysetu_test -e "SHOW TABLES;"

# Drop test database
mysql -u root -p -e "DROP DATABASE samaysetu_test;"
```

## 📋 Pre-Migration Checklist

Before migrating to Supabase:

- [ ] Full database backup created
- [ ] Backup file verified (not empty)
- [ ] CSV exports created (if using CSV import method)
- [ ] Record counts noted for verification
- [ ] Backup stored in safe location
- [ ] Test restore performed (optional)

## 📊 Get Record Counts

```sql
-- Connect to MySQL
mysql -u root -p samaysetu

-- Get counts for all tables
SELECT 'teachers' as table_name, COUNT(*) as count FROM teachers
UNION ALL
SELECT 'academic_years', COUNT(*) FROM academic_years
UNION ALL
SELECT 'departments', COUNT(*) FROM departments
UNION ALL
SELECT 'courses', COUNT(*) FROM courses
UNION ALL
SELECT 'divisions', COUNT(*) FROM divisions
UNION ALL
SELECT 'batches', COUNT(*) FROM batches
UNION ALL
SELECT 'time_slots', COUNT(*) FROM time_slots
UNION ALL
SELECT 'class_rooms', COUNT(*) FROM class_rooms;
```

## 🚨 Troubleshooting

### Error: Access Denied
```bash
# Make sure you're using correct credentials
# Check application.properties for actual username/password
```

### Error: mysqldump not found
```bash
# Add MySQL bin directory to PATH
# Or use full path:
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqldump.exe" -u root -p samaysetu > backup.sql
```

### Error: secure_file_priv
```sql
-- Check allowed directory
SHOW VARIABLES LIKE 'secure_file_priv';

-- Use that directory for CSV exports
-- Or disable it (not recommended for production):
-- Add to my.ini: secure_file_priv=""
```

### Error: File already exists
```bash
# Delete old backup or use different filename
del mysql_backups\samaysetu_full_backup.sql
```

## 💡 Tips

1. **Always backup before migration** - Better safe than sorry!
2. **Keep multiple backups** - Use timestamps in filenames
3. **Verify backups** - Check file size and content
4. **Store safely** - Keep backups in multiple locations
5. **Document counts** - Note record counts for verification

## 📞 Next Steps

After successful backup:
1. ✅ Verify backup files exist and are not empty
2. ✅ Note down record counts for each table
3. ✅ Follow `Files/SUPABASE_MIGRATION_GUIDE.md`
4. ✅ Create Supabase project
5. ✅ Run clean schema script
6. ✅ Import data to Supabase

---

**Backup Guide for MIT Academy of Engineering**

© 2024 MIT Academy of Engineering - SamaySetu Development Team
