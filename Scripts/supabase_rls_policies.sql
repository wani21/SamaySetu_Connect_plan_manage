-- =====================================================
-- SUPABASE ROW LEVEL SECURITY (RLS) POLICIES
-- =====================================================
-- This script sets up Row Level Security for SamaySetu
-- to ensure data access is properly controlled
--
-- IMPORTANT: Run this AFTER the main migration script
-- =====================================================

-- =====================================================
-- STEP 1: ENABLE RLS ON ALL TABLES
-- =====================================================

ALTER TABLE academic_years ENABLE ROW LEVEL SECURITY;
ALTER TABLE departments ENABLE ROW LEVEL SECURITY;
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE divisions ENABLE ROW LEVEL SECURITY;
ALTER TABLE batches ENABLE ROW LEVEL SECURITY;
ALTER TABLE courses ENABLE ROW LEVEL SECURITY;
ALTER TABLE classrooms ENABLE ROW LEVEL SECURITY;
ALTER TABLE time_slots ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_availability ENABLE ROW LEVEL SECURITY;
ALTER TABLE students ENABLE ROW LEVEL SECURITY;
ALTER TABLE teacher_courses ENABLE ROW LEVEL SECURITY;
ALTER TABLE timetable_entries ENABLE ROW LEVEL SECURITY;

-- =====================================================
-- STEP 2: CREATE HELPER FUNCTION TO GET CURRENT USER
-- =====================================================

-- Function to get current user's role from JWT
CREATE OR REPLACE FUNCTION get_user_role()
RETURNS TEXT AS $$
BEGIN
  -- In Supabase, you can access JWT claims via auth.jwt()
  -- For Spring Boot backend, we'll use service role key
  -- This function can be customized based on your auth setup
  RETURN current_setting('app.current_user_role', TRUE);
EXCEPTION
  WHEN OTHERS THEN
    RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to get current user's ID
CREATE OR REPLACE FUNCTION get_user_id()
RETURNS BIGINT AS $$
BEGIN
  RETURN current_setting('app.current_user_id', TRUE)::BIGINT;
EXCEPTION
  WHEN OTHERS THEN
    RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =====================================================
-- STEP 3: ACADEMIC YEARS POLICIES
-- =====================================================

-- Allow all authenticated users to read academic years
CREATE POLICY "academic_years_select_policy"
ON academic_years FOR SELECT
USING (true);

-- Only admins can insert academic years
CREATE POLICY "academic_years_insert_policy"
ON academic_years FOR INSERT
WITH CHECK (get_user_role() = 'ADMIN');

-- Only admins can update academic years
CREATE POLICY "academic_years_update_policy"
ON academic_years FOR UPDATE
USING (get_user_role() = 'ADMIN');

-- Only admins can delete academic years
CREATE POLICY "academic_years_delete_policy"
ON academic_years FOR DELETE
USING (get_user_role() = 'ADMIN');

-- =====================================================
-- STEP 4: DEPARTMENTS POLICIES
-- =====================================================

-- Allow all authenticated users to read departments
CREATE POLICY "departments_select_policy"
ON departments FOR SELECT
USING (true);

-- Only admins can insert departments
CREATE POLICY "departments_insert_policy"
ON departments FOR INSERT
WITH CHECK (get_user_role() = 'ADMIN');

-- Only admins can update departments
CREATE POLICY "departments_update_policy"
ON departments FOR UPDATE
USING (get_user_role() = 'ADMIN');

-- Only admins can delete departments
CREATE POLICY "departments_delete_policy"
ON departments FOR DELETE
USING (get_user_role() = 'ADMIN');

-- =====================================================
-- STEP 5: USERS POLICIES
-- =====================================================

-- Users can read their own data, admins can read all
CREATE POLICY "users_select_policy"
ON users FOR SELECT
USING (
  get_user_role() = 'ADMIN' OR 
  id = get_user_id()
);

-- Only admins can insert users
CREATE POLICY "users_insert_policy"
ON users FOR INSERT
WITH CHECK (get_user_role() = 'ADMIN');

-- Users can update their own profile, admins can update all
CREATE POLICY "users_update_policy"
ON users FOR UPDATE
USING (
  get_user_role() = 'ADMIN' OR 
  id = get_user_id()
);

-- Only admins can delete users
CREATE POLICY "users_delete_policy"
ON users FOR DELETE
USING (get_user_role() = 'ADMIN');

-- =====================================================
-- STEP 6: DIVISIONS POLICIES
-- =====================================================

-- Allow all authenticated users to read divisions
CREATE POLICY "divisions_select_policy"
ON divisions FOR SELECT
USING (true);

-- Only admins can insert divisions
CREATE POLICY "divisions_insert_policy"
ON divisions FOR INSERT
WITH CHECK (get_user_role() = 'ADMIN');

-- Only admins can update divisions
CREATE POLICY "divisions_update_policy"
ON divisions FOR UPDATE
USING (get_user_role() = 'ADMIN');

-- Only admins can delete divisions
CREATE POLICY "divisions_delete_policy"
ON divisions FOR DELETE
USING (get_user_role() = 'ADMIN');

-- =====================================================
-- STEP 7: BATCHES POLICIES
-- =====================================================

-- Allow all authenticated users to read batches
CREATE POLICY "batches_select_policy"
ON batches FOR SELECT
USING (true);

-- Only admins can insert batches
CREATE POLICY "batches_insert_policy"
ON batches FOR INSERT
WITH CHECK (get_user_role() = 'ADMIN');

-- Only admins can update batches
CREATE POLICY "batches_update_policy"
ON batches FOR UPDATE
USING (get_user_role() = 'ADMIN');

-- Only admins can delete batches
CREATE POLICY "batches_delete_policy"
ON batches FOR DELETE
USING (get_user_role() = 'ADMIN');

-- =====================================================
-- STEP 8: COURSES POLICIES
-- =====================================================

-- Allow all authenticated users to read courses
CREATE POLICY "courses_select_policy"
ON courses FOR SELECT
USING (true);

-- Only admins can insert courses
CREATE POLICY "courses_insert_policy"
ON courses FOR INSERT
WITH CHECK (get_user_role() = 'ADMIN');

-- Only admins can update courses
CREATE POLICY "courses_update_policy"
ON courses FOR UPDATE
USING (get_user_role() = 'ADMIN');

-- Only admins can delete courses
CREATE POLICY "courses_delete_policy"
ON courses FOR DELETE
USING (get_user_role() = 'ADMIN');

-- =====================================================
-- STEP 9: CLASSROOMS POLICIES
-- =====================================================

-- Allow all authenticated users to read classrooms
CREATE POLICY "classrooms_select_policy"
ON classrooms FOR SELECT
USING (true);

-- Only admins can insert classrooms
CREATE POLICY "classrooms_insert_policy"
ON classrooms FOR INSERT
WITH CHECK (get_user_role() = 'ADMIN');

-- Only admins can update classrooms
CREATE POLICY "classrooms_update_policy"
ON classrooms FOR UPDATE
USING (get_user_role() = 'ADMIN');

-- Only admins can delete classrooms
CREATE POLICY "classrooms_delete_policy"
ON classrooms FOR DELETE
USING (get_user_role() = 'ADMIN');

-- =====================================================
-- STEP 10: TIME SLOTS POLICIES
-- =====================================================

-- Allow all authenticated users to read time slots
CREATE POLICY "time_slots_select_policy"
ON time_slots FOR SELECT
USING (true);

-- Only admins can insert time slots
CREATE POLICY "time_slots_insert_policy"
ON time_slots FOR INSERT
WITH CHECK (get_user_role() = 'ADMIN');

-- Only admins can update time slots
CREATE POLICY "time_slots_update_policy"
ON time_slots FOR UPDATE
USING (get_user_role() = 'ADMIN');

-- Only admins can delete time slots
CREATE POLICY "time_slots_delete_policy"
ON time_slots FOR DELETE
USING (get_user_role() = 'ADMIN');

-- =====================================================
-- STEP 11: USER AVAILABILITY POLICIES
-- =====================================================

-- Users can read their own availability, admins can read all
CREATE POLICY "user_availability_select_policy"
ON user_availability FOR SELECT
USING (
  get_user_role() = 'ADMIN' OR 
  user_id = get_user_id()
);

-- Users can insert their own availability, admins can insert for anyone
CREATE POLICY "user_availability_insert_policy"
ON user_availability FOR INSERT
WITH CHECK (
  get_user_role() = 'ADMIN' OR 
  user_id = get_user_id()
);

-- Users can update their own availability, admins can update all
CREATE POLICY "user_availability_update_policy"
ON user_availability FOR UPDATE
USING (
  get_user_role() = 'ADMIN' OR 
  user_id = get_user_id()
);

-- Users can delete their own availability, admins can delete all
CREATE POLICY "user_availability_delete_policy"
ON user_availability FOR DELETE
USING (
  get_user_role() = 'ADMIN' OR 
  user_id = get_user_id()
);

-- =====================================================
-- STEP 12: STUDENTS POLICIES
-- =====================================================

-- Allow all authenticated users to read students
CREATE POLICY "students_select_policy"
ON students FOR SELECT
USING (true);

-- Only admins can insert students
CREATE POLICY "students_insert_policy"
ON students FOR INSERT
WITH CHECK (get_user_role() = 'ADMIN');

-- Only admins can update students
CREATE POLICY "students_update_policy"
ON students FOR UPDATE
USING (get_user_role() = 'ADMIN');

-- Only admins can delete students
CREATE POLICY "students_delete_policy"
ON students FOR DELETE
USING (get_user_role() = 'ADMIN');

-- =====================================================
-- STEP 13: TEACHER COURSES POLICIES
-- =====================================================

-- Allow all authenticated users to read teacher-course mappings
CREATE POLICY "teacher_courses_select_policy"
ON teacher_courses FOR SELECT
USING (true);

-- Only admins can insert teacher-course mappings
CREATE POLICY "teacher_courses_insert_policy"
ON teacher_courses FOR INSERT
WITH CHECK (get_user_role() = 'ADMIN');

-- Only admins can delete teacher-course mappings
CREATE POLICY "teacher_courses_delete_policy"
ON teacher_courses FOR DELETE
USING (get_user_role() = 'ADMIN');

-- =====================================================
-- STEP 14: TIMETABLE ENTRIES POLICIES
-- =====================================================

-- Allow all authenticated users to read timetable entries
CREATE POLICY "timetable_entries_select_policy"
ON timetable_entries FOR SELECT
USING (true);

-- Only admins can insert timetable entries
CREATE POLICY "timetable_entries_insert_policy"
ON timetable_entries FOR INSERT
WITH CHECK (get_user_role() = 'ADMIN');

-- Only admins can update timetable entries
CREATE POLICY "timetable_entries_update_policy"
ON timetable_entries FOR UPDATE
USING (get_user_role() = 'ADMIN');

-- Only admins can delete timetable entries
CREATE POLICY "timetable_entries_delete_policy"
ON timetable_entries FOR DELETE
USING (get_user_role() = 'ADMIN');

-- =====================================================
-- STEP 15: GRANT SERVICE ROLE BYPASS
-- =====================================================

-- Grant service role (used by Spring Boot) to bypass RLS
-- This allows your backend to perform all operations
GRANT ALL ON ALL TABLES IN SCHEMA public TO service_role;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO service_role;
GRANT ALL ON ALL FUNCTIONS IN SCHEMA public TO service_role;

-- =====================================================
-- VERIFICATION QUERIES
-- =====================================================

-- Check which tables have RLS enabled
SELECT schemaname, tablename, rowsecurity 
FROM pg_tables 
WHERE schemaname = 'public' 
ORDER BY tablename;

-- Check all policies
SELECT schemaname, tablename, policyname, permissive, roles, cmd, qual
FROM pg_policies
WHERE schemaname = 'public'
ORDER BY tablename, policyname;

-- =====================================================
-- RLS SETUP COMPLETE!
-- =====================================================
-- 
-- IMPORTANT NOTES:
-- 
-- 1. RLS is now enabled on all tables
-- 2. Service role (used by Spring Boot) bypasses RLS
-- 3. If using Supabase Auth, update helper functions
-- 4. Policies enforce:
--    - Admins: Full access to everything
--    - Teachers: Can read most data, manage own availability
--    - All users: Can read academic structure data
-- 
-- 5. To use RLS with Spring Boot:
--    - Use service_role key in application.properties
--    - Or set session variables before queries:
--      SET app.current_user_id = 123;
--      SET app.current_user_role = 'ADMIN';
-- 
-- =====================================================
