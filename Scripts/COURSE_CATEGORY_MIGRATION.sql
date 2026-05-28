-- =====================================================
-- COURSE CATEGORY MIGRATION SCRIPT
-- =====================================================
-- Purpose: Add course_category field to courses table
-- Options: CORE, NORMAL_ELECTIVE, GLOBAL_ELECTIVE
-- Default: CORE (for existing courses)
-- =====================================================

-- Step 1: Add the course_category column (nullable initially)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'courses' 
        AND column_name = 'course_category'
    ) THEN
        ALTER TABLE courses 
        ADD COLUMN course_category VARCHAR(50);
        
        RAISE NOTICE 'Column course_category added to courses table';
    ELSE
        RAISE NOTICE 'Column course_category already exists in courses table';
    END IF;
END $$;

-- Step 2: Set default value for existing courses
DO $$
DECLARE
    updated_count INTEGER;
BEGIN
    UPDATE courses 
    SET course_category = 'CORE' 
    WHERE course_category IS NULL;
    
    GET DIAGNOSTICS updated_count = ROW_COUNT;
    
    RAISE NOTICE 'Set default course_category = CORE for % existing courses', updated_count;
END $$;

-- Step 3: Make the column NOT NULL
DO $$
BEGIN
    ALTER TABLE courses 
    ALTER COLUMN course_category SET NOT NULL;
    
    RAISE NOTICE 'Column course_category set to NOT NULL';
END $$;

-- Step 4: Add check constraint to ensure only valid values
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.constraint_column_usage 
        WHERE table_name = 'courses' 
        AND constraint_name = 'courses_category_check'
    ) THEN
        ALTER TABLE courses 
        ADD CONSTRAINT courses_category_check 
        CHECK (course_category IN ('CORE', 'NORMAL_ELECTIVE', 'GLOBAL_ELECTIVE'));
        
        RAISE NOTICE 'Check constraint added for course_category';
    ELSE
        RAISE NOTICE 'Check constraint already exists for course_category';
    END IF;
END $$;

-- Step 5: Verify the migration
DO $$
DECLARE
    total_courses INTEGER;
    core_count INTEGER;
    normal_elective_count INTEGER;
    global_elective_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO total_courses FROM courses;
    SELECT COUNT(*) INTO core_count FROM courses WHERE course_category = 'CORE';
    SELECT COUNT(*) INTO normal_elective_count FROM courses WHERE course_category = 'NORMAL_ELECTIVE';
    SELECT COUNT(*) INTO global_elective_count FROM courses WHERE course_category = 'GLOBAL_ELECTIVE';
    
    RAISE NOTICE '=== MIGRATION SUMMARY ===';
    RAISE NOTICE 'Total courses: %', total_courses;
    RAISE NOTICE 'CORE courses: %', core_count;
    RAISE NOTICE 'NORMAL_ELECTIVE courses: %', normal_elective_count;
    RAISE NOTICE 'GLOBAL_ELECTIVE courses: %', global_elective_count;
    RAISE NOTICE '========================';
END $$;

-- SUCCESS MESSAGE
DO $$
BEGIN
    RAISE NOTICE 'SUCCESS: Course category migration completed successfully!';
    RAISE NOTICE 'All existing courses have been set to CORE category by default.';
    RAISE NOTICE 'You can now update individual courses to NORMAL_ELECTIVE or GLOBAL_ELECTIVE as needed.';
END $$;
