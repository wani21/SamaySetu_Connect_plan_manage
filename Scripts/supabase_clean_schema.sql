-- ============================================
-- SamaySetu Clean Schema for Supabase PostgreSQL
-- ============================================
-- This script creates a clean, properly formatted database schema
-- with improved table names and structure for Supabase

-- Drop existing tables if they exist (in correct order due to foreign keys)
DROP TABLE IF EXISTS timetable_entries CASCADE;
DROP TABLE IF EXISTS user_availability CASCADE;
DROP TABLE IF EXISTS batches CASCADE;
DROP TABLE IF EXISTS divisions CASCADE;
DROP TABLE IF EXISTS courses CASCADE;
DROP TABLE IF EXISTS classrooms CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS departments CASCADE;
DROP TABLE IF EXISTS time_slots CASCADE;
DROP TABLE IF EXISTS academic_years CASCADE;

-- ============================================
-- 1. ACADEMIC YEARS TABLE
-- ============================================
CREATE TABLE academic_years (
    id BIGSERIAL PRIMARY KEY,
    year_name VARCHAR(20) NOT NULL UNIQUE,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_current BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_year_dates CHECK (end_date > start_date)
);

-- Index for current year lookup
CREATE INDEX idx_academic_years_current ON academic_years(is_current) WHERE is_current = true;

-- ============================================
-- 2. DEPARTMENTS TABLE
-- ============================================
CREATE TABLE departments (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(10) NOT NULL,
    head_of_department VARCHAR(100),
    years VARCHAR(20) DEFAULT '1,2,3,4',
    academic_year_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_dept_academic_year FOREIGN KEY (academic_year_id) 
        REFERENCES academic_years(id) ON DELETE SET NULL,
    CONSTRAINT uk_dept_code_year UNIQUE (code, academic_year_id),
    CONSTRAINT uk_dept_name_year UNIQUE (name, academic_year_id)
);

-- Indexes for departments
CREATE INDEX idx_departments_academic_year ON departments(academic_year_id);
CREATE INDEX idx_departments_code ON departments(code);

-- ============================================
-- 3. USERS TABLE (formerly teachers)
-- ============================================
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    employee_id VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(15),
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'TEACHER',
    specialization TEXT,
    weekly_hours_limit INTEGER DEFAULT 25,
    is_active BOOLEAN DEFAULT true,
    is_email_verified BOOLEAN DEFAULT false,
    is_first_login BOOLEAN DEFAULT true,
    email_verification_token VARCHAR(255),
    password_reset_token VARCHAR(255),
    password_reset_expires TIMESTAMP,
    department_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_department FOREIGN KEY (department_id) 
        REFERENCES departments(id) ON DELETE SET NULL,
    CONSTRAINT chk_role CHECK (role IN ('ADMIN', 'TEACHER', 'STUDENT')),
    CONSTRAINT chk_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
);

-- Indexes for users
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_employee_id ON users(employee_id);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_department ON users(department_id);
CREATE INDEX idx_users_active ON users(is_active) WHERE is_active = true;

-- ============================================
-- 4. TIME SLOTS TABLE
-- ============================================
CREATE TABLE time_slots (
    id BIGSERIAL PRIMARY KEY,
    slot_name VARCHAR(50) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    type VARCHAR(20) DEFAULT 'TYPE_1',
    is_break BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_time_order CHECK (end_time > start_time),
    CONSTRAINT chk_slot_type CHECK (type IN ('TYPE_1', 'TYPE_2'))
);

-- Indexes for time slots
CREATE INDEX idx_time_slots_type ON time_slots(type);
CREATE INDEX idx_time_slots_active ON time_slots(is_active) WHERE is_active = true;
CREATE INDEX idx_time_slots_break ON time_slots(is_break);

-- ============================================
-- 5. DIVISIONS TABLE
-- ============================================
CREATE TABLE divisions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(10) NOT NULL,
    year INTEGER NOT NULL,
    branch VARCHAR(100),
    total_students INTEGER DEFAULT 0,
    time_slot_type VARCHAR(20) DEFAULT 'TYPE_1',
    class_teacher VARCHAR(100),
    class_representative VARCHAR(100),
    is_active BOOLEAN DEFAULT true,
    department_id BIGINT NOT NULL,
    academic_year_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_division_department FOREIGN KEY (department_id) 
        REFERENCES departments(id) ON DELETE CASCADE,
    CONSTRAINT fk_division_academic_year FOREIGN KEY (academic_year_id) 
        REFERENCES academic_years(id) ON DELETE CASCADE,
    CONSTRAINT chk_year_range CHECK (year BETWEEN 1 AND 4),
    CONSTRAINT chk_division_slot_type CHECK (time_slot_type IN ('TYPE_1', 'TYPE_2')),
    CONSTRAINT uk_division_name_dept_year UNIQUE (name, department_id, academic_year_id, year)
);

-- Indexes for divisions
CREATE INDEX idx_divisions_department ON divisions(department_id);
CREATE INDEX idx_divisions_academic_year ON divisions(academic_year_id);
CREATE INDEX idx_divisions_year ON divisions(year);
CREATE INDEX idx_divisions_active ON divisions(is_active) WHERE is_active = true;

-- ============================================
-- 6. BATCHES TABLE
-- ============================================
CREATE TABLE batches (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    division_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_batch_division FOREIGN KEY (division_id) 
        REFERENCES divisions(id) ON DELETE CASCADE,
    CONSTRAINT uk_batch_name_division UNIQUE (name, division_id)
);

-- Index for batches
CREATE INDEX idx_batches_division ON batches(division_id);

-- ============================================
-- 7. COURSES TABLE
-- ============================================
CREATE TABLE courses (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(20) NOT NULL,
    course_type VARCHAR(20) NOT NULL,
    credits INTEGER NOT NULL,
    hours_per_week INTEGER NOT NULL,
    semester VARCHAR(10) NOT NULL,
    year INTEGER,
    description TEXT,
    prerequisites TEXT,
    is_active BOOLEAN DEFAULT true,
    department_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_course_department FOREIGN KEY (department_id) 
        REFERENCES departments(id) ON DELETE CASCADE,
    CONSTRAINT chk_course_type CHECK (course_type IN ('THEORY', 'LAB', 'TUTORIAL')),
    CONSTRAINT chk_semester CHECK (semester IN ('SEM_1', 'SEM_2', 'SEM_3', 'SEM_4', 'SEM_5', 'SEM_6', 'SEM_7', 'SEM_8')),
    CONSTRAINT chk_course_year CHECK (year BETWEEN 1 AND 4),
    CONSTRAINT chk_credits CHECK (credits > 0),
    CONSTRAINT chk_hours CHECK (hours_per_week > 0)
);

-- Indexes for courses
CREATE INDEX idx_courses_department ON courses(department_id);
CREATE INDEX idx_courses_code ON courses(code);
CREATE INDEX idx_courses_semester ON courses(semester);
CREATE INDEX idx_courses_year ON courses(year);
CREATE INDEX idx_courses_active ON courses(is_active) WHERE is_active = true;

-- ============================================
-- 8. CLASSROOMS TABLE (formerly class_rooms)
-- ============================================
CREATE TABLE classrooms (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    room_number VARCHAR(20) NOT NULL UNIQUE,
    capacity INTEGER DEFAULT 0,
    room_type VARCHAR(20) NOT NULL,
    has_projector BOOLEAN DEFAULT false,
    has_ac BOOLEAN DEFAULT false,
    equipment TEXT,
    building VARCHAR(50),
    wing VARCHAR(20),
    floor INTEGER,
    is_active BOOLEAN DEFAULT true,
    department_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_classroom_department FOREIGN KEY (department_id) 
        REFERENCES departments(id) ON DELETE SET NULL,
    CONSTRAINT chk_room_type CHECK (room_type IN ('CLASSROOM', 'LAB', 'AUDITORIUM', 'SEMINAR_HALL')),
    CONSTRAINT chk_capacity CHECK (capacity >= 0),
    CONSTRAINT chk_floor CHECK (floor >= 0)
);

-- Indexes for classrooms
CREATE INDEX idx_classrooms_department ON classrooms(department_id);
CREATE INDEX idx_classrooms_type ON classrooms(room_type);
CREATE INDEX idx_classrooms_building ON classrooms(building);
CREATE INDEX idx_classrooms_active ON classrooms(is_active) WHERE is_active = true;

-- ============================================
-- 9. USER AVAILABILITY TABLE (formerly teacher_availability)
-- ============================================
CREATE TABLE user_availability (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    day_of_week VARCHAR(10) NOT NULL,
    time_slot_id BIGINT NOT NULL,
    is_available BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_availability_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_availability_time_slot FOREIGN KEY (time_slot_id) 
        REFERENCES time_slots(id) ON DELETE CASCADE,
    CONSTRAINT chk_day_of_week CHECK (day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY')),
    CONSTRAINT uk_user_day_slot UNIQUE (user_id, day_of_week, time_slot_id)
);

-- Indexes for user availability
CREATE INDEX idx_availability_user ON user_availability(user_id);
CREATE INDEX idx_availability_day ON user_availability(day_of_week);
CREATE INDEX idx_availability_slot ON user_availability(time_slot_id);

-- ============================================
-- 10. TIMETABLE ENTRIES TABLE
-- ============================================
CREATE TABLE timetable_entries (
    id BIGSERIAL PRIMARY KEY,
    division_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    user_id BIGINT,
    classroom_id BIGINT,
    time_slot_id BIGINT NOT NULL,
    day_of_week VARCHAR(10) NOT NULL,
    batch_id BIGINT,
    academic_year_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_timetable_division FOREIGN KEY (division_id) 
        REFERENCES divisions(id) ON DELETE CASCADE,
    CONSTRAINT fk_timetable_course FOREIGN KEY (course_id) 
        REFERENCES courses(id) ON DELETE CASCADE,
    CONSTRAINT fk_timetable_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_timetable_classroom FOREIGN KEY (classroom_id) 
        REFERENCES classrooms(id) ON DELETE SET NULL,
    CONSTRAINT fk_timetable_time_slot FOREIGN KEY (time_slot_id) 
        REFERENCES time_slots(id) ON DELETE CASCADE,
    CONSTRAINT fk_timetable_batch FOREIGN KEY (batch_id) 
        REFERENCES batches(id) ON DELETE SET NULL,
    CONSTRAINT fk_timetable_academic_year FOREIGN KEY (academic_year_id) 
        REFERENCES academic_years(id) ON DELETE CASCADE,
    CONSTRAINT chk_timetable_day CHECK (day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'))
);

-- Indexes for timetable entries
CREATE INDEX idx_timetable_division ON timetable_entries(division_id);
CREATE INDEX idx_timetable_course ON timetable_entries(course_id);
CREATE INDEX idx_timetable_user ON timetable_entries(user_id);
CREATE INDEX idx_timetable_classroom ON timetable_entries(classroom_id);
CREATE INDEX idx_timetable_time_slot ON timetable_entries(time_slot_id);
CREATE INDEX idx_timetable_day ON timetable_entries(day_of_week);
CREATE INDEX idx_timetable_academic_year ON timetable_entries(academic_year_id);

-- ============================================
-- TRIGGERS FOR UPDATED_AT
-- ============================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create triggers for all tables
CREATE TRIGGER update_academic_years_updated_at BEFORE UPDATE ON academic_years
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_departments_updated_at BEFORE UPDATE ON departments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_time_slots_updated_at BEFORE UPDATE ON time_slots
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_divisions_updated_at BEFORE UPDATE ON divisions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_batches_updated_at BEFORE UPDATE ON batches
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_courses_updated_at BEFORE UPDATE ON courses
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_classrooms_updated_at BEFORE UPDATE ON classrooms
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_availability_updated_at BEFORE UPDATE ON user_availability
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_timetable_entries_updated_at BEFORE UPDATE ON timetable_entries
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- HELPER FUNCTIONS
-- ============================================

-- Function to get current academic year
CREATE OR REPLACE FUNCTION get_current_academic_year()
RETURNS TABLE (
    id BIGINT,
    year_name VARCHAR,
    start_date DATE,
    end_date DATE
) AS $$
BEGIN
    RETURN QUERY
    SELECT ay.id, ay.year_name, ay.start_date, ay.end_date
    FROM academic_years ay
    WHERE ay.is_current = true
    LIMIT 1;
END;
$$ LANGUAGE plpgsql;

-- Function to check time slot overlap
CREATE OR REPLACE FUNCTION check_time_slot_overlap(
    p_start_time TIME,
    p_end_time TIME,
    p_type VARCHAR,
    p_exclude_id BIGINT DEFAULT NULL
)
RETURNS BOOLEAN AS $$
DECLARE
    overlap_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO overlap_count
    FROM time_slots
    WHERE type = p_type
    AND (p_exclude_id IS NULL OR id != p_exclude_id)
    AND (
        (start_time <= p_start_time AND end_time > p_start_time)
        OR (start_time < p_end_time AND end_time >= p_end_time)
        OR (start_time >= p_start_time AND end_time <= p_end_time)
    );
    
    RETURN overlap_count > 0;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- INITIAL DATA
-- ============================================

-- Insert default admin user (password: admin123)
INSERT INTO users (
    name, employee_id, email, phone, password, role,
    specialization, is_active, is_email_verified, is_first_login,
    weekly_hours_limit
) VALUES (
    'System Administrator',
    'ADMIN001',
    'admin@mitaoe.ac.in',
    '9999999999',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
    'ADMIN',
    'System Administration',
    true,
    true,
    false,
    40
);

-- ============================================
-- COMMENTS FOR DOCUMENTATION
-- ============================================

COMMENT ON TABLE academic_years IS 'Stores academic year information';
COMMENT ON TABLE departments IS 'Stores department information linked to academic years';
COMMENT ON TABLE users IS 'Stores all user information (teachers, admins, students)';
COMMENT ON TABLE time_slots IS 'Stores time slot definitions for scheduling';
COMMENT ON TABLE divisions IS 'Stores class divisions/sections';
COMMENT ON TABLE batches IS 'Stores student batches within divisions';
COMMENT ON TABLE courses IS 'Stores course/subject information';
COMMENT ON TABLE classrooms IS 'Stores classroom/lab information';
COMMENT ON TABLE user_availability IS 'Stores user availability for scheduling';
COMMENT ON TABLE timetable_entries IS 'Stores actual timetable schedule entries';

-- ============================================
-- GRANT PERMISSIONS (if needed)
-- ============================================

-- Grant permissions to authenticated users (Supabase)
-- GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO authenticated;
-- GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO authenticated;

-- ============================================
-- SCHEMA CREATION COMPLETE
-- ============================================

-- Verify table creation
SELECT 
    table_name,
    (SELECT COUNT(*) FROM information_schema.columns WHERE table_name = t.table_name) as column_count
FROM information_schema.tables t
WHERE table_schema = 'public'
AND table_type = 'BASE TABLE'
ORDER BY table_name;

COMMENT ON SCHEMA public IS 'SamaySetu Clean Schema - Created for Supabase PostgreSQL';
