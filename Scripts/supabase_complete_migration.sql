-- =====================================================
-- SUPABASE COMPLETE MIGRATION SCRIPT FOR SAMAYSETU
-- =====================================================
-- This script creates a clean PostgreSQL schema and migrates
-- all data from MySQL backup to Supabase
--
-- Database: samaysetu
-- Source: MySQL 8.0.42
-- Target: PostgreSQL (Supabase)
-- Migration Date: 2026-01-31
--
-- INSTRUCTIONS:
-- 1. Create a new Supabase project at https://supabase.com
-- 2. Go to SQL Editor in Supabase Dashboard
-- 3. Copy and paste this entire script
-- 4. Execute the script
-- 5. Verify data migration using the verification queries at the end
--
-- =====================================================

-- Enable UUID extension (useful for future features)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =====================================================
-- STEP 1: DROP EXISTING TABLES (if any)
-- =====================================================

DROP TABLE IF EXISTS timetable_entries CASCADE;
DROP TABLE IF EXISTS teacher_courses CASCADE;
DROP TABLE IF EXISTS user_availability CASCADE;
DROP TABLE IF EXISTS students CASCADE;
DROP TABLE IF EXISTS batches CASCADE;
DROP TABLE IF EXISTS courses CASCADE;
DROP TABLE IF EXISTS divisions CASCADE;
DROP TABLE IF EXISTS classrooms CASCADE;
DROP TABLE IF EXISTS departments CASCADE;
DROP TABLE IF EXISTS academic_years CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- =====================================================
-- STEP 2: CREATE CLEAN SCHEMA WITH PROPER TABLE NAMES
-- =====================================================

-- Table: academic_years
-- Stores academic year information
CREATE TABLE academic_years (
    id BIGSERIAL PRIMARY KEY,
    year_name VARCHAR(20) NOT NULL UNIQUE,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_current BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table: departments
-- Stores department information linked to academic years
CREATE TABLE departments (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(10) NOT NULL,
    head_of_department VARCHAR(100),
    years VARCHAR(255),
    academic_year_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (academic_year_id) REFERENCES academic_years(id) ON DELETE CASCADE,
    UNIQUE (code, academic_year_id),
    UNIQUE (name, academic_year_id)
);

-- Table: users (renamed from teachers)
-- Stores all user accounts (teachers, admin, staff)
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    employee_id VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(15),
    role VARCHAR(50) DEFAULT 'TEACHER',
    specialization TEXT,
    department_id BIGINT,
    is_active BOOLEAN DEFAULT TRUE,
    is_approved BOOLEAN DEFAULT FALSE,
    is_email_verified BOOLEAN DEFAULT FALSE,
    is_first_login BOOLEAN DEFAULT TRUE,
    weekly_hours_limit INT CHECK (weekly_hours_limit BETWEEN 1 AND 40),
    min_weekly_hours INT CHECK (min_weekly_hours BETWEEN 1 AND 40),
    max_weekly_hours INT CHECK (max_weekly_hours BETWEEN 1 AND 50),
    verification_token VARCHAR(255),
    verification_token_expiry TIMESTAMP,
    password_reset_token VARCHAR(255),
    password_reset_token_expiry TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL
);

-- Table: divisions
-- Stores division/class information
CREATE TABLE divisions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(10) NOT NULL,
    branch VARCHAR(50) NOT NULL,
    year INT NOT NULL CHECK (year BETWEEN 1 AND 4),
    total_students INT DEFAULT 0 CHECK (total_students >= 0),
    time_slot_type VARCHAR(20),
    class_teacher VARCHAR(100),
    class_representative VARCHAR(100),
    is_active BOOLEAN DEFAULT TRUE,
    academic_year_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (academic_year_id) REFERENCES academic_years(id) ON DELETE CASCADE,
    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE CASCADE
);

-- Table: batches
-- Stores batch information within divisions
CREATE TABLE batches (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    division_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (division_id) REFERENCES divisions(id) ON DELETE CASCADE
);

-- Table: courses
-- Stores course information
CREATE TABLE courses (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    course_type VARCHAR(20) NOT NULL CHECK (course_type IN ('LAB', 'THEORY')),
    semester VARCHAR(20) NOT NULL CHECK (semester IN ('SEM_1', 'SEM_2', 'SEM_3', 'SEM_4', 'SEM_5', 'SEM_6', 'SEM_7', 'SEM_8')),
    year INT,
    credits INT NOT NULL CHECK (credits >= 1),
    hours_per_week INT NOT NULL CHECK (hours_per_week >= 1),
    description TEXT,
    prerequisites TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    department_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE CASCADE
);

-- Table: classrooms (renamed from rooms)
-- Stores classroom/lab information
CREATE TABLE classrooms (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    room_number VARCHAR(20) NOT NULL UNIQUE,
    room_type VARCHAR(20) NOT NULL CHECK (room_type IN ('CLASSROOM', 'LAB', 'AUDITORIUM')),
    capacity INT NOT NULL CHECK (capacity >= 1),
    building_wing VARCHAR(10),
    has_projector BOOLEAN DEFAULT FALSE,
    has_ac BOOLEAN DEFAULT FALSE,
    equipment TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    department_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL
);

-- Table: time_slots
-- Stores time slot information for schedules
CREATE TABLE time_slots (
    id BIGSERIAL PRIMARY KEY,
    slot_name VARCHAR(50),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    duration_minutes INT NOT NULL CHECK (duration_minutes >= 1),
    type VARCHAR(20),
    is_break BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table: user_availability (renamed from teacher_availability)
-- Stores user availability information
CREATE TABLE user_availability (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    day_of_week VARCHAR(20) NOT NULL CHECK (day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY')),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_available BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Table: students
-- Stores student information
CREATE TABLE students (
    id BIGSERIAL PRIMARY KEY,
    roll_number VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE,
    phone VARCHAR(15),
    admission_year INT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    division_id BIGINT,
    batch_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (division_id) REFERENCES divisions(id) ON DELETE SET NULL,
    FOREIGN KEY (batch_id) REFERENCES batches(id) ON DELETE SET NULL
);

-- Table: teacher_courses (junction table)
-- Links users to courses they teach
CREATE TABLE teacher_courses (
    user_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, course_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
);

-- Table: timetable_entries
-- Stores timetable entries
CREATE TABLE timetable_entries (
    id BIGSERIAL PRIMARY KEY,
    academic_year_id BIGINT NOT NULL,
    division_id BIGINT NOT NULL,
    batch_id BIGINT,
    course_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    classroom_id BIGINT NOT NULL,
    time_slot_id BIGINT NOT NULL,
    day_of_week VARCHAR(20) NOT NULL CHECK (day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY')),
    week_number INT CHECK (week_number >= 1),
    is_recurring BOOLEAN DEFAULT TRUE,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (academic_year_id) REFERENCES academic_years(id) ON DELETE CASCADE,
    FOREIGN KEY (division_id) REFERENCES divisions(id) ON DELETE CASCADE,
    FOREIGN KEY (batch_id) REFERENCES batches(id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (classroom_id) REFERENCES classrooms(id) ON DELETE CASCADE,
    FOREIGN KEY (time_slot_id) REFERENCES time_slots(id) ON DELETE CASCADE
);

-- =====================================================
-- STEP 3: CREATE INDEXES FOR PERFORMANCE
-- =====================================================

CREATE INDEX idx_departments_academic_year ON departments(academic_year_id);
CREATE INDEX idx_users_department ON users(department_id);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_employee_id ON users(employee_id);
CREATE INDEX idx_divisions_academic_year ON divisions(academic_year_id);
CREATE INDEX idx_divisions_department ON divisions(department_id);
CREATE INDEX idx_batches_division ON batches(division_id);
CREATE INDEX idx_courses_department ON courses(department_id);
CREATE INDEX idx_classrooms_department ON classrooms(department_id);
CREATE INDEX idx_user_availability_user ON user_availability(user_id);
CREATE INDEX idx_students_division ON students(division_id);
CREATE INDEX idx_students_batch ON students(batch_id);
CREATE INDEX idx_timetable_academic_year ON timetable_entries(academic_year_id);
CREATE INDEX idx_timetable_division ON timetable_entries(division_id);
CREATE INDEX idx_timetable_user ON timetable_entries(user_id);
CREATE INDEX idx_timetable_day ON timetable_entries(day_of_week);

-- =====================================================
-- STEP 4: INSERT DATA FROM MYSQL BACKUP
-- =====================================================

-- Insert academic_years data
INSERT INTO academic_years (id, year_name, start_date, end_date, is_current, created_at) VALUES
(1, '2024-25', '2024-07-01', '2025-06-30', FALSE, '2025-12-03 05:05:16.001600'),
(2, '2025-26', '2025-08-01', '2026-07-31', TRUE, '2025-12-04 00:18:39.841236'),
(6, '2026-27', '2026-07-01', '2027-06-30', FALSE, '2025-12-24 01:25:05.916367');

-- Insert departments data
INSERT INTO departments (id, code, name, head_of_department, years, academic_year_id, created_at, updated_at) VALUES
(1, 'CS', 'Computer Engineering', 'Dr. Pramod Ganjewar', '1,2,3,4', 2, '2025-12-03 03:30:08.014312', '2025-12-24 00:36:28.177387'),
(2, 'CSE(AI-ML)', 'Computer Science (AI-ML)', 'Dr. Diptee Ghusse', '1,2', 2, '2025-12-03 22:41:50.853612', '2025-12-24 01:43:58.805513'),
(5, 'MECH', 'Mechanical Department', 'Mrs. Sample User', '1,2,3,4', 1, '2025-12-24 01:44:57.125235', '2025-12-24 01:44:57.125235'),
(15, 'CS', 'Computer Engineering', 'Dr. Pramod Ganjewar', '1,2,3,4', 6, '2025-12-24 12:20:09.150576', '2025-12-24 12:20:09.150576'),
(16, 'CSE(AI-ML)', 'Computer Science (AI-ML)', 'Dr. Diptee Ghusse', '1,2', 6, '2025-12-24 12:20:09.357710', '2025-12-24 12:20:09.357710');

-- Insert users data (from teachers table)
INSERT INTO users (id, employee_id, name, email, password, phone, role, specialization, department_id, is_active, is_approved, is_email_verified, is_first_login, weekly_hours_limit, min_weekly_hours, max_weekly_hours, created_at, updated_at) VALUES
(6, 'ADMIN001', 'System Administrator', 'admin@mitaoe.ac.in', '$2a$10$oHHl0gDPqlr/G/9Vi1OvBe8v.J9GUq2X1PJIVu5mn.FdoVQU9jypa', '6243973641', 'ADMIN', 'System Administration', 1, TRUE, TRUE, TRUE, FALSE, 40, 25, 30, '2025-12-03 01:50:11.000000', '2025-12-23 23:49:50.864767'),
(10, 'EMP101', 'Mr. Abhijeet Rane', '202301040228@mitaoe.ac.in', '$2a$10$UPr7/KKhmb7g80Zljx3rQurOLx6/rHUKc70PxAvCzVd1K1Kl6kgUW', '9325872534', 'TEACHER', 'Cloud Computing', 1, TRUE, TRUE, TRUE, FALSE, 25, 25, 30, '2025-12-04 03:45:04.952854', '2025-12-24 01:22:24.164952'),
(11, 'EMP010', 'Mrs. Sample User', 'sampleuser@mitaoe.ac.in', '$2a$10$raksEqnnTSN4Hg.WE/ejge7S6U4x.GQk0XwdNu.QnvAxXQjoJ.DI.', '9876543210', 'TEACHER', 'Computer Science', 2, TRUE, TRUE, TRUE, FALSE, NULL, 25, 30, '2025-12-22 01:57:34.727096', '2025-12-24 01:22:37.043994');

-- Insert divisions data
INSERT INTO divisions (id, name, branch, year, total_students, time_slot_type, class_teacher, class_representative, is_active, academic_year_id, department_id, created_at, updated_at) VALUES
(2, 'A', 'Computer Science', 3, 85, 'TYPE_2', 'Mrs. Sample User', 'demo Teacher', TRUE, 2, 1, '2025-12-04 02:06:19.849654', '2025-12-24 01:46:21.593896'),
(3, 'B', 'Computer Science', 3, 85, 'TYPE_1', NULL, NULL, TRUE, 2, 1, '2025-12-04 02:36:30.826232', '2025-12-04 02:36:30.826232'),
(4, 'C', 'Computer Science', 3, 85, 'TYPE_1', NULL, NULL, TRUE, 2, 1, '2025-12-04 02:36:39.989073', '2025-12-04 02:36:47.551460'),
(5, 'A', 'CS (AI-ML)', 2, 85, 'TYPE_1', NULL, NULL, TRUE, 2, 2, '2025-12-04 02:37:37.337993', '2025-12-04 02:37:37.337993'),
(6, 'C1', 'Computer Engineering', 1, 85, 'TYPE_1', NULL, NULL, TRUE, 2, 1, '2025-12-24 00:38:02.791300', '2025-12-24 00:38:02.791300');

-- Insert batches data
INSERT INTO batches (id, name, division_id, created_at, updated_at) VALUES
(1, 'A1', 2, '2025-12-23 18:23:13', '2025-12-23 18:23:13'),
(2, 'A2', 2, '2025-12-23 18:23:19', '2025-12-23 18:23:19'),
(3, 'A3', 2, '2025-12-23 18:23:24', '2025-12-23 18:23:24'),
(4, 'B1', 3, '2025-12-23 18:23:31', '2025-12-23 18:23:31'),
(5, 'B2', 3, '2025-12-23 18:23:36', '2025-12-23 18:23:36'),
(6, 'C1', 4, '2025-12-23 18:23:58', '2025-12-23 18:23:58');

-- Insert courses data
INSERT INTO courses (id, code, name, course_type, semester, year, credits, hours_per_week, description, prerequisites, is_active, department_id, created_at, updated_at) VALUES
(4, 'CS534', 'OS', 'THEORY', 'SEM_5', 3, 3, 3, NULL, NULL, TRUE, 1, '2025-12-24 01:08:33.408699', '2025-12-24 01:09:24.995505');

-- Insert classrooms data (from rooms table)
INSERT INTO classrooms (id, name, room_number, room_type, capacity, building_wing, has_projector, has_ac, equipment, is_active, department_id, created_at, updated_at) VALUES
(1, 'Project Lab', 'H304', 'LAB', 40, 'H', TRUE, TRUE, NULL, TRUE, 1, '2025-12-04 00:29:45.333273', '2025-12-04 00:29:45.333273'),
(3, 'Room H301', 'H301', 'CLASSROOM', 90, 'H', TRUE, FALSE, NULL, TRUE, 1, '2025-12-04 00:35:49.315806', '2025-12-04 00:35:49.315806'),
(6, 'Computer Department Library', 'H201', 'AUDITORIUM', 30, 'H', TRUE, TRUE, 'Books', TRUE, 1, '2025-12-04 00:45:42.099623', '2025-12-04 00:45:42.099623'),
(26, 'Room H302', 'H302', 'CLASSROOM', 90, 'H', TRUE, FALSE, NULL, TRUE, 1, '2025-12-04 01:44:01.491732', '2025-12-04 01:44:01.491732');

-- Insert time_slots data
INSERT INTO time_slots (id, slot_name, start_time, end_time, duration_minutes, type, is_break, is_active, created_at) VALUES
(1, 'Lecture 1', '08:30:00', '09:25:00', 55, 'TYPE_1', FALSE, TRUE, '2025-12-04 01:48:29.341054'),
(2, 'Lecture 2', '09:25:00', '10:20:00', 55, 'TYPE_1', FALSE, TRUE, '2025-12-04 01:49:04.197879'),
(3, 'Short Break', '10:20:00', '10:30:00', 10, 'TYPE_1', TRUE, TRUE, '2025-12-04 01:49:44.015713'),
(4, 'Lecture 3', '10:30:00', '11:25:00', 55, 'TYPE_1', FALSE, TRUE, '2025-12-04 01:50:13.862131'),
(5, 'Lunch Break', '11:25:00', '12:20:00', 55, 'TYPE_1', TRUE, TRUE, '2025-12-04 01:50:50.507276'),
(6, 'Lecture 4', '12:20:00', '13:15:00', 55, 'TYPE_1', FALSE, TRUE, '2025-12-04 01:51:24.213582'),
(7, 'Lecture 5', '13:15:00', '14:10:00', 55, 'TYPE_1', FALSE, TRUE, '2025-12-04 01:51:49.437223'),
(8, 'Lecture 6', '14:10:00', '15:05:00', 55, 'TYPE_1', FALSE, TRUE, '2025-12-04 01:52:09.885015'),
(9, 'Short Break', '15:05:00', '15:10:00', 5, 'TYPE_1', TRUE, TRUE, '2025-12-04 01:52:36.863433'),
(10, 'Lecture 7', '15:10:00', '16:00:00', 50, 'TYPE_1', FALSE, TRUE, '2025-12-04 01:53:16.676022'),
(11, 'Lecture 8', '16:00:00', '16:50:00', 50, 'TYPE_1', FALSE, TRUE, '2025-12-04 01:53:45.186245'),
(12, 'Lecture 1', '08:30:00', '09:25:00', 55, 'TYPE_2', FALSE, TRUE, '2025-12-23 22:36:23.276775'),
(13, 'Lecture 2', '09:25:00', '10:20:00', 55, 'TYPE_2', FALSE, TRUE, '2025-12-23 23:46:13.099031'),
(14, 'Lecture 3', '10:30:00', '11:25:00', 55, 'TYPE_2', FALSE, TRUE, '2025-12-23 23:46:49.082723'),
(15, 'Short Break', '10:20:00', '10:30:00', 10, 'TYPE_2', TRUE, TRUE, '2025-12-23 23:47:17.347378'),
(16, 'Lecture 4', '11:25:00', '12:20:00', 55, 'TYPE_2', FALSE, TRUE, '2025-12-26 19:35:09.100237'),
(17, 'Lunch Break', '12:20:00', '13:15:00', 55, 'TYPE_2', TRUE, TRUE, '2025-12-26 19:35:45.966116'),
(18, 'Lecture 5', '13:15:00', '14:10:00', 55, 'TYPE_2', FALSE, TRUE, '2025-12-26 19:36:16.317620'),
(19, 'Lecture 6', '14:10:00', '15:05:00', 55, 'TYPE_2', FALSE, TRUE, '2025-12-26 19:37:07.306256'),
(20, 'Short Break', '15:05:00', '15:10:00', 5, 'TYPE_2', TRUE, TRUE, '2025-12-26 19:37:40.582868'),
(21, 'Lecture 7', '15:10:00', '16:00:00', 50, 'TYPE_2', FALSE, TRUE, '2025-12-26 19:38:19.199147'),
(22, 'Lecture 8', '16:00:00', '16:50:00', 50, 'TYPE_2', FALSE, TRUE, '2025-12-26 19:39:29.710416');

-- =====================================================
-- STEP 5: RESET SEQUENCES TO CORRECT VALUES
-- =====================================================

SELECT setval('academic_years_id_seq', (SELECT MAX(id) FROM academic_years));
SELECT setval('departments_id_seq', (SELECT MAX(id) FROM departments));
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
SELECT setval('divisions_id_seq', (SELECT MAX(id) FROM divisions));
SELECT setval('batches_id_seq', (SELECT MAX(id) FROM batches));
SELECT setval('courses_id_seq', (SELECT MAX(id) FROM courses));
SELECT setval('classrooms_id_seq', (SELECT MAX(id) FROM classrooms));
SELECT setval('time_slots_id_seq', (SELECT MAX(id) FROM time_slots));

-- =====================================================
-- STEP 6: VERIFICATION QUERIES
-- =====================================================

-- Check record counts
SELECT 'academic_years' as table_name, COUNT(*) as record_count FROM academic_years
UNION ALL
SELECT 'departments', COUNT(*) FROM departments
UNION ALL
SELECT 'users', COUNT(*) FROM users
UNION ALL
SELECT 'divisions', COUNT(*) FROM divisions
UNION ALL
SELECT 'batches', COUNT(*) FROM batches
UNION ALL
SELECT 'courses', COUNT(*) FROM courses
UNION ALL
SELECT 'classrooms', COUNT(*) FROM classrooms
UNION ALL
SELECT 'time_slots', COUNT(*) FROM time_slots
UNION ALL
SELECT 'students', COUNT(*) FROM students
UNION ALL
SELECT 'user_availability', COUNT(*) FROM user_availability
UNION ALL
SELECT 'teacher_courses', COUNT(*) FROM teacher_courses
UNION ALL
SELECT 'timetable_entries', COUNT(*) FROM timetable_entries;

-- =====================================================
-- MIGRATION COMPLETE!
-- =====================================================
-- Expected record counts:
-- academic_years: 3
-- departments: 5
-- users: 3
-- divisions: 5
-- batches: 6
-- courses: 1
-- classrooms: 4
-- time_slots: 22
-- students: 0
-- user_availability: 0
-- teacher_courses: 0
-- timetable_entries: 0
-- =====================================================
