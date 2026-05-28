-- =====================================================
-- COURSE SHORT NAME UPDATE SCRIPT
-- =====================================================
-- Purpose: Update short_name column to allow spaces, hyphens, and increase max length to 15
-- Changes:
--   - Increase column length from 10 to 15
--   - Update check constraint to allow spaces and hyphens
--   - Pattern: ^[A-Z0-9 -]{2,15}$
-- =====================================================

-- Step 1: Drop existing check constraint
ALTER TABLE courses
DROP CONSTRAINT IF EXISTS check_course_short_name_format;

-- Step 2: Increase column length to 15
ALTER TABLE courses
ALTER COLUMN short_name TYPE VARCHAR(15);

-- Step 3: Add updated check constraint (allows A-Z, a-z, 0-9, space, hyphen)
ALTER TABLE courses
ADD CONSTRAINT check_course_short_name_format 
CHECK (short_name ~ '^[A-Za-z0-9 -]{2,15}$');

-- Step 4: Verify the changes
DO $$
DECLARE
    total_courses INT;
    invalid_courses INT;
BEGIN
    SELECT COUNT(*) INTO total_courses FROM courses;
    
    -- Check for any courses that don't match the new pattern
    SELECT COUNT(*) INTO invalid_courses 
    FROM courses 
    WHERE short_name !~ '^[A-Za-z0-9 -]{2,15}$';
    
    RAISE NOTICE '========================================';
    RAISE NOTICE 'SHORT NAME UPDATE SUMMARY';
    RAISE NOTICE '========================================';
    RAISE NOTICE 'Total Courses: %', total_courses;
    RAISE NOTICE 'Invalid Short Names: %', invalid_courses;
    
    IF invalid_courses > 0 THEN
        RAISE NOTICE 'WARNING: % courses have invalid short names', invalid_courses;
        RAISE NOTICE 'Run this query to see them:';
        RAISE NOTICE 'SELECT id, name, short_name FROM courses WHERE short_name !~ ''^[A-Za-z0-9 -]{2,15}$'';';
    ELSE
        RAISE NOTICE 'SUCCESS: All courses have valid short names';
    END IF;
    
    RAISE NOTICE '========================================';
END $$;

-- =====================================================
-- UPDATE COMPLETE
-- =====================================================
-- Next Steps:
-- 1. Restart your backend application
-- 2. Test creating/editing courses with spaces and hyphens in short names
-- 3. Verify timetable displays work correctly
-- =====================================================
