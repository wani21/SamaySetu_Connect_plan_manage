-- ============================================
-- Export MySQL Data to CSV for Supabase Migration
-- ============================================
-- Run this script in MySQL to export all data to CSV files
-- These CSV files can then be imported into Supabase

-- Note: You may need to adjust the output path based on your MySQL configuration
-- Default path: /var/lib/mysql-files/ (MySQL 8.0+)
-- Or use: SHOW VARIABLES LIKE 'secure_file_priv'; to find the allowed directory

-- ============================================
-- 1. EXPORT ACADEMIC YEARS
-- ============================================
SELECT 'Exporting academic_years...' as status;

SELECT 
    id,
    year_name,
    start_date,
    end_date,
    is_current,
    created_at,
    updated_at
INTO OUTFILE '/var/lib/mysql-files/academic_years.csv'
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
FROM academic_years;

-- ============================================
-- 2. EXPORT DEPARTMENTS
-- ============================================
SELECT 'Exporting departments...' as status;

SELECT 
    id,
    name,
    code,
    head_of_department,
    years,
    academic_year_id,
    created_at,
    updated_at
INTO OUTFILE '/var/lib/mysql-files/departments.csv'
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
FROM departments;

-- ============================================
-- 3. EXPORT TEACHERS (will become users)
-- ============================================
SELECT 'Exporting teachers...' as status;

SELECT 
    id,
    name,
    employee_id,
    email,
    phone,
    password,
    role,
    specialization,
    weekly_hours_limit,
    is_active,
    is_email_verified,
    COALESCE(is_first_login, 1) as is_first_login,
    department_id,
    created_at,
    updated_at
INTO OUTFILE '/var/lib/mysql-files/teachers.csv'
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
FROM teachers;

-- ============================================
-- 4. EXPORT TIME SLOTS
-- ============================================
SELECT 'Exporting time_slots...' as status;

SELECT 
    id,
    slot_name,
    start_time,
    end_time,
    COALESCE(type, 'TYPE_1') as type,
    COALESCE(is_break, 0) as is_break,
    COALESCE(is_active, 1) as is_active,
    created_at,
    updated_at
INTO OUTFILE '/var/lib/mysql-files/time_slots.csv'
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
FROM time_slots;

-- ============================================
-- 5. EXPORT DIVISIONS
-- ============================================
SELECT 'Exporting divisions...' as status;

SELECT 
    id,
    name,
    year,
    branch,
    total_students,
    COALESCE(time_slot_type, 'TYPE_1') as time_slot_type,
    class_teacher,
    class_representative,
    COALESCE(is_active, 1) as is_active,
    department_id,
    academic_year_id,
    created_at,
    updated_at
INTO OUTFILE '/var/lib/mysql-files/divisions.csv'
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
FROM divisions;

-- ============================================
-- 6. EXPORT BATCHES
-- ============================================
SELECT 'Exporting batches...' as status;

SELECT 
    id,
    name,
    division_id,
    created_at,
    updated_at
INTO OUTFILE '/var/lib/mysql-files/batches.csv'
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
FROM batches;

-- ============================================
-- 7. EXPORT COURSES
-- ============================================
SELECT 'Exporting courses...' as status;

SELECT 
    id,
    name,
    code,
    course_type,
    credits,
    hours_per_week,
    semester,
    year,
    description,
    prerequisites,
    COALESCE(is_active, 1) as is_active,
    department_id,
    created_at,
    updated_at
INTO OUTFILE '/var/lib/mysql-files/courses.csv'
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
FROM courses;

-- ============================================
-- 8. EXPORT CLASSROOMS (from class_rooms)
-- ============================================
SELECT 'Exporting class_rooms...' as status;

SELECT 
    id,
    name,
    room_number,
    capacity,
    room_type,
    COALESCE(has_projector, 0) as has_projector,
    COALESCE(has_ac, 0) as has_ac,
    equipment,
    building,
    wing,
    floor,
    COALESCE(is_active, 1) as is_active,
    department_id,
    created_at,
    updated_at
INTO OUTFILE '/var/lib/mysql-files/class_rooms.csv'
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
FROM class_rooms;

-- ============================================
-- EXPORT SUMMARY
-- ============================================
SELECT 'Export completed!' as status;

SELECT 
    'academic_years' as table_name, 
    COUNT(*) as records_exported 
FROM academic_years
UNION ALL
SELECT 'departments', COUNT(*) FROM departments
UNION ALL
SELECT 'teachers', COUNT(*) FROM teachers
UNION ALL
SELECT 'time_slots', COUNT(*) FROM time_slots
UNION ALL
SELECT 'divisions', COUNT(*) FROM divisions
UNION ALL
SELECT 'batches', COUNT(*) FROM batches
UNION ALL
SELECT 'courses', COUNT(*) FROM courses
UNION ALL
SELECT 'class_rooms', COUNT(*) FROM class_rooms;

-- ============================================
-- ALTERNATIVE: Export with Headers
-- ============================================
-- If you need CSV files with headers, use this approach:

-- Academic Years with header
-- (SELECT 'id', 'year_name', 'start_date', 'end_date', 'is_current', 'created_at', 'updated_at')
-- UNION ALL
-- (SELECT id, year_name, start_date, end_date, is_current, created_at, updated_at FROM academic_years)
-- INTO OUTFILE '/var/lib/mysql-files/academic_years_with_header.csv'
-- FIELDS TERMINATED BY ',' ENCLOSED BY '"' LINES TERMINATED BY '\n';

-- ============================================
-- NOTES
-- ============================================
-- 1. Files will be created in: /var/lib/mysql-files/
-- 2. If you get "secure_file_priv" error, check: SHOW VARIABLES LIKE 'secure_file_priv';
-- 3. Make sure MySQL user has FILE privilege: GRANT FILE ON *.* TO 'user'@'localhost';
-- 4. After export, copy files to a location accessible for Supabase import
-- 5. You can also use mysqldump as an alternative:
--    mysqldump -u user -p --tab=/path/to/output samaysetu_db

-- ============================================
-- VERIFICATION
-- ============================================
-- After export, verify files exist:
-- ls -lh /var/lib/mysql-files/*.csv

-- Check file contents:
-- head -n 5 /var/lib/mysql-files/academic_years.csv
