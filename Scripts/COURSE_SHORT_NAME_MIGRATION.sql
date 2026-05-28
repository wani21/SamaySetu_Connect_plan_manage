-- =====================================================
-- COURSE SHORT NAME MIGRATION SCRIPT
-- =====================================================
-- Purpose: Add short_name column to courses table and populate with unique values
-- Uniqueness Scope: Department + Year (e.g., "DS" can exist in CS and IT for same year)
-- Strategy: Use course code if available, otherwise first 6 chars of name (uppercase, remove spaces)
-- Duplicate Handling: Add alphabetic suffixes (DS, DSA, DSB, etc.)
-- =====================================================

-- Step 1: Add short_name column (nullable initially for migration)
ALTER TABLE courses 
ADD COLUMN IF NOT EXISTS short_name VARCHAR(10);

-- Step 2: Create a temporary function to generate short names
CREATE OR REPLACE FUNCTION generate_course_short_name(
    p_course_code VARCHAR(20),
    p_course_name VARCHAR(100)
) RETURNS VARCHAR(10) AS $$
DECLARE
    v_short_name VARCHAR(10);
BEGIN
    -- Try to use course code first (uppercase, remove spaces, limit to 10 chars)
    IF p_course_code IS NOT NULL AND LENGTH(TRIM(p_course_code)) > 0 THEN
        v_short_name := UPPER(REPLACE(TRIM(p_course_code), ' ', ''));
        v_short_name := SUBSTRING(v_short_name, 1, 10);
    ELSE
        -- Fallback: Use first 6 chars of course name (uppercase, remove spaces)
        v_short_name := UPPER(REPLACE(TRIM(p_course_name), ' ', ''));
        v_short_name := SUBSTRING(v_short_name, 1, 6);
    END IF;
    
    RETURN v_short_name;
END;
$$ LANGUAGE plpgsql;

-- Step 3: Populate short names with uniqueness handling
DO $$
DECLARE
    course_record RECORD;
    base_short_name VARCHAR(10);
    final_short_name VARCHAR(10);
    suffix_char CHAR(1);
    attempt_count INT;
    is_unique BOOLEAN;
BEGIN
    -- Loop through all courses ordered by department, year, and id
    FOR course_record IN 
        SELECT c.id, c.code, c.name, c.department_id, c.year
        FROM courses c
        ORDER BY c.department_id, c.year, c.id
    LOOP
        -- Generate base short name
        base_short_name := generate_course_short_name(course_record.code, course_record.name);
        final_short_name := base_short_name;
        attempt_count := 0;
        is_unique := FALSE;
        
        -- Check uniqueness within department and year
        WHILE NOT is_unique AND attempt_count < 26 LOOP
            -- Check if this short name already exists for this department and year
            IF EXISTS (
                SELECT 1 FROM courses 
                WHERE UPPER(short_name) = UPPER(final_short_name)
                AND department_id = course_record.department_id
                AND year = course_record.year
                AND id != course_record.id
            ) THEN
                -- Not unique, try adding suffix
                suffix_char := CHR(65 + attempt_count); -- A=65, B=66, etc.
                final_short_name := SUBSTRING(base_short_name, 1, 9) || suffix_char;
                attempt_count := attempt_count + 1;
            ELSE
                -- Unique! Exit loop
                is_unique := TRUE;
            END IF;
        END LOOP;
        
        -- Update the course with the unique short name
        UPDATE courses 
        SET short_name = final_short_name
        WHERE id = course_record.id;
        
        RAISE NOTICE 'Course ID %: % -> Short Name: %', 
            course_record.id, course_record.name, final_short_name;
    END LOOP;
END $$;

-- Step 4: Verify all courses have short names
DO $$
DECLARE
    null_count INT;
BEGIN
    SELECT COUNT(*) INTO null_count
    FROM courses
    WHERE short_name IS NULL OR short_name = '';
    
    IF null_count > 0 THEN
        RAISE EXCEPTION 'Migration failed: % courses still have NULL or empty short names', null_count;
    ELSE
        RAISE NOTICE 'SUCCESS: All courses have valid short names';
    END IF;
END $$;

-- Step 5: Make short_name NOT NULL
ALTER TABLE courses 
ALTER COLUMN short_name SET NOT NULL;

-- Step 6: Add unique constraint on (short_name, department_id, year)
-- Note: Using case-insensitive comparison via expression index
CREATE UNIQUE INDEX IF NOT EXISTS idx_courses_short_name_dept_year 
ON courses (UPPER(short_name), department_id, year);

-- Step 7: Add check constraint for format validation (2-10 alphanumeric uppercase)
ALTER TABLE courses
ADD CONSTRAINT check_course_short_name_format 
CHECK (short_name ~ '^[A-Z0-9]{2,10}$');

-- Step 8: Drop the temporary function
DROP FUNCTION IF EXISTS generate_course_short_name(VARCHAR, VARCHAR);

-- Step 9: Display summary
DO $$
DECLARE
    total_courses INT;
    unique_short_names INT;
    course_rec RECORD;
BEGIN
    SELECT COUNT(*) INTO total_courses FROM courses;
    SELECT COUNT(DISTINCT UPPER(short_name) || '-' || department_id || '-' || year) 
    INTO unique_short_names FROM courses;
    
    RAISE NOTICE '========================================';
    RAISE NOTICE 'MIGRATION SUMMARY';
    RAISE NOTICE '========================================';
    RAISE NOTICE 'Total Courses: %', total_courses;
    RAISE NOTICE 'Unique Short Names (Dept+Year): %', unique_short_names;
    RAISE NOTICE '========================================';
    
    -- Show sample of generated short names
    RAISE NOTICE 'Sample Short Names:';
    FOR course_rec IN 
        SELECT name, short_name, department_id, year
        FROM courses
        ORDER BY department_id, year, id
        LIMIT 10
    LOOP
        RAISE NOTICE '  % (Dept: %, Year: %) -> %', 
            course_rec.name, course_rec.department_id, 
            course_rec.year, course_rec.short_name;
    END LOOP;
END $$;

-- =====================================================
-- MIGRATION COMPLETE
-- =====================================================
-- Next Steps:
-- 1. Review the generated short names
-- 2. Manually adjust any short names that don't make sense
-- 3. Restart your application to pick up the schema changes
-- =====================================================
