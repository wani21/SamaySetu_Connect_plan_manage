-- ============================================
-- SamaySetu Data Migration Script
-- MySQL to Supabase PostgreSQL
-- ============================================
-- This script helps migrate data from MySQL to Supabase
-- Run this AFTER creating the clean schema

-- ============================================
-- STEP 1: PREPARE FOR DATA IMPORT
-- ============================================

-- Disable triggers temporarily for faster import
ALTER TABLE academic_years DISABLE TRIGGER ALL;
ALTER TABLE departments DISABLE TRIGGER ALL;
ALTER TABLE users DISABLE TRIGGER ALL;
ALTER TABLE time_slots DISABLE TRIGGER ALL;
ALTER TABLE divisions DISABLE TRIGGER ALL;
ALTER TABLE batches DISABLE TRIGGER ALL;
ALTER TABLE courses DISABLE TRIGGER ALL;
ALTER TABLE classrooms DISABLE TRIGGER ALL;

-- ============================================
-- STEP 2: IMPORT DATA (Use COPY or INSERT)
-- ============================================

-- Method 1: Using COPY command (fastest)
-- Copy CSV files exported from MySQL

-- Academic Years
\COPY academic_years(id, year_name, start_date, end_date, is_current, created_at, updated_at) 
FROM '/path/to/academic_years.csv' 
DELIMITER ',' 
CSV HEADER;

-- Departments
\COPY departments(id, name, code, head_of_department, years, academic_year_id, created_at, updated_at) 
FROM '/path/to/departments.csv' 
DELIMITER ',' 
CSV HEADER;

-- Users (from teachers table)
\COPY users(id, name, employee_id, email, phone, password, role, specialization, weekly_hours_limit, is_active, is_email_verified, is_first_login, department_id, created_at, updated_at) 
FROM '/path/to/teachers.csv' 
DELIMITER ',' 
CSV HEADER;

-- Time Slots
\COPY time_slots(id, slot_name, start_time, end_time, type, is_break, is_active, created_at, updated_at) 
FROM '/path/to/time_slots.csv' 
DELIMITER ',' 
CSV HEADER;

-- Divisions
\COPY divisions(id, name, year, branch, total_students, time_slot_type, class_teacher, class_representative, is_active, department_id, academic_year_id, created_at, updated_at) 
FROM '/path/to/divisions.csv' 
DELIMITER ',' 
CSV HEADER;

-- Batches
\COPY batches(id, name, division_id, created_at, updated_at) 
FROM '/path/to/batches.csv' 
DELIMITER ',' 
CSV HEADER;

-- Courses
\COPY courses(id, name, code, course_type, credits, hours_per_week, semester, year, description, prerequisites, is_active, department_id, created_at, updated_at) 
FROM '/path/to/courses.csv' 
DELIMITER ',' 
CSV HEADER;

-- Classrooms (from class_rooms table)
\COPY classrooms(id, name, room_number, capacity, room_type, has_projector, has_ac, equipment, building, wing, floor, is_active, department_id, created_at, updated_at) 
FROM '/path/to/class_rooms.csv' 
DELIMITER ',' 
CSV HEADER;

-- ============================================
-- STEP 3: RESET SEQUENCES
-- ============================================
-- Important: Reset sequences to avoid ID conflicts

SELECT setval('academic_years_id_seq', (SELECT COALESCE(MAX(id), 1) FROM academic_years));
SELECT setval('departments_id_seq', (SELECT COALESCE(MAX(id), 1) FROM departments));
SELECT setval('users_id_seq', (SELECT COALESCE(MAX(id), 1) FROM users));
SELECT setval('time_slots_id_seq', (SELECT COALESCE(MAX(id), 1) FROM time_slots));
SELECT setval('divisions_id_seq', (SELECT COALESCE(MAX(id), 1) FROM divisions));
SELECT setval('batches_id_seq', (SELECT COALESCE(MAX(id), 1) FROM batches));
SELECT setval('courses_id_seq', (SELECT COALESCE(MAX(id), 1) FROM courses));
SELECT setval('classrooms_id_seq', (SELECT COALESCE(MAX(id), 1) FROM classrooms));

-- ============================================
-- STEP 4: RE-ENABLE TRIGGERS
-- ============================================

ALTER TABLE academic_years ENABLE TRIGGER ALL;
ALTER TABLE departments ENABLE TRIGGER ALL;
ALTER TABLE users ENABLE TRIGGER ALL;
ALTER TABLE time_slots ENABLE TRIGGER ALL;
ALTER TABLE divisions ENABLE TRIGGER ALL;
ALTER TABLE batches ENABLE TRIGGER ALL;
ALTER TABLE courses ENABLE TRIGGER ALL;
ALTER TABLE classrooms ENABLE TRIGGER ALL;

-- ============================================
-- STEP 5: VERIFY DATA MIGRATION
-- ============================================

-- Check record counts
SELECT 'academic_years' as table_name, COUNT(*) as count FROM academic_years
UNION ALL
SELECT 'departments', COUNT(*) FROM departments
UNION ALL
SELECT 'users', COUNT(*) FROM users
UNION ALL
SELECT 'time_slots', COUNT(*) FROM time_slots
UNION ALL
SELECT 'divisions', COUNT(*) FROM divisions
UNION ALL
SELECT 'batches', COUNT(*) FROM batches
UNION ALL
SELECT 'courses', COUNT(*) FROM courses
UNION ALL
SELECT 'classrooms', COUNT(*) FROM classrooms
ORDER BY table_name;

-- Check for NULL foreign keys (potential issues)
SELECT 'departments with NULL academic_year_id' as issue, COUNT(*) as count
FROM departments WHERE academic_year_id IS NULL
UNION ALL
SELECT 'users with NULL department_id', COUNT(*)
FROM users WHERE department_id IS NULL
UNION ALL
SELECT 'divisions with NULL department_id', COUNT(*)
FROM divisions WHERE department_id IS NULL
UNION ALL
SELECT 'divisions with NULL academic_year_id', COUNT(*)
FROM divisions WHERE academic_year_id IS NULL
UNION ALL
SELECT 'courses with NULL department_id', COUNT(*)
FROM courses WHERE department_id IS NULL;

-- Check for duplicate emails
SELECT email, COUNT(*) as count
FROM users
GROUP BY email
HAVING COUNT(*) > 1;

-- Check for duplicate employee IDs
SELECT employee_id, COUNT(*) as count
FROM users
GROUP BY employee_id
HAVING COUNT(*) > 1;

-- ============================================
-- STEP 6: DATA CLEANUP (if needed)
-- ============================================

-- Remove any test/invalid data
-- DELETE FROM users WHERE email LIKE '%test%' AND role != 'ADMIN';

-- Update NULL values if needed
-- UPDATE users SET is_first_login = true WHERE is_first_login IS NULL;
-- UPDATE users SET is_active = true WHERE is_active IS NULL;
-- UPDATE users SET is_email_verified = false WHERE is_email_verified IS NULL;

-- Fix any data inconsistencies
-- UPDATE courses SET is_active = true WHERE is_active IS NULL;
-- UPDATE time_slots SET is_active = true WHERE is_active IS NULL;

-- ============================================
-- STEP 7: ANALYZE TABLES
-- ============================================
-- Update statistics for query optimizer

ANALYZE academic_years;
ANALYZE departments;
ANALYZE users;
ANALYZE time_slots;
ANALYZE divisions;
ANALYZE batches;
ANALYZE courses;
ANALYZE classrooms;

-- ============================================
-- STEP 8: VERIFY FOREIGN KEY RELATIONSHIPS
-- ============================================

-- Check departments → academic_years
SELECT d.id, d.name, d.academic_year_id, ay.year_name
FROM departments d
LEFT JOIN academic_years ay ON d.academic_year_id = ay.id
WHERE d.academic_year_id IS NOT NULL
LIMIT 10;

-- Check users → departments
SELECT u.id, u.name, u.department_id, d.name as department_name
FROM users u
LEFT JOIN departments d ON u.department_id = d.id
WHERE u.department_id IS NOT NULL
LIMIT 10;

-- Check divisions → departments and academic_years
SELECT div.id, div.name, div.department_id, d.name as department_name, 
       div.academic_year_id, ay.year_name
FROM divisions div
LEFT JOIN departments d ON div.department_id = d.id
LEFT JOIN academic_years ay ON div.academic_year_id = ay.id
LIMIT 10;

-- Check courses → departments
SELECT c.id, c.name, c.department_id, d.name as department_name
FROM courses c
LEFT JOIN departments d ON c.department_id = d.id
LIMIT 10;

-- ============================================
-- STEP 9: CREATE SAMPLE DATA (if needed)
-- ============================================

-- If you need to create sample data for testing

-- Sample Academic Year
-- INSERT INTO academic_years (year_name, start_date, end_date, is_current)
-- VALUES ('2024-25', '2024-07-01', '2025-06-30', true);

-- Sample Department
-- INSERT INTO departments (name, code, head_of_department, years, academic_year_id)
-- VALUES ('Computer Engineering', 'COMP', 'Dr. John Smith', '1,2,3,4', 1);

-- Sample Time Slots
-- INSERT INTO time_slots (slot_name, start_time, end_time, type, is_break)
-- VALUES 
--     ('Period 1', '09:00:00', '10:00:00', 'TYPE_1', false),
--     ('Period 2', '10:00:00', '11:00:00', 'TYPE_1', false),
--     ('Break', '11:00:00', '11:15:00', 'TYPE_1', true),
--     ('Period 3', '11:15:00', '12:15:00', 'TYPE_1', false),
--     ('Lunch', '12:15:00', '13:00:00', 'TYPE_1', true);

-- ============================================
-- STEP 10: FINAL VERIFICATION
-- ============================================

-- Get summary of all tables
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size,
    pg_stat_get_live_tuples(c.oid) AS row_count
FROM pg_tables t
JOIN pg_class c ON t.tablename = c.relname
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- Check indexes
SELECT 
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE schemaname = 'public'
ORDER BY tablename, indexname;

-- ============================================
-- MIGRATION COMPLETE
-- ============================================

-- Display success message
DO $$
BEGIN
    RAISE NOTICE '============================================';
    RAISE NOTICE 'Data Migration Completed Successfully!';
    RAISE NOTICE '============================================';
    RAISE NOTICE 'Next Steps:';
    RAISE NOTICE '1. Verify data integrity';
    RAISE NOTICE '2. Test application connectivity';
    RAISE NOTICE '3. Update Spring Boot configuration';
    RAISE NOTICE '4. Test all API endpoints';
    RAISE NOTICE '============================================';
END $$;

-- Show final counts
SELECT 
    'Total Users: ' || COUNT(*) as summary FROM users
UNION ALL
SELECT 'Total Departments: ' || COUNT(*) FROM departments
UNION ALL
SELECT 'Total Courses: ' || COUNT(*) FROM courses
UNION ALL
SELECT 'Total Divisions: ' || COUNT(*) FROM divisions
UNION ALL
SELECT 'Total Batches: ' || COUNT(*) FROM batches
UNION ALL
SELECT 'Total Time Slots: ' || COUNT(*) FROM time_slots
UNION ALL
SELECT 'Total Classrooms: ' || COUNT(*) FROM classrooms;
