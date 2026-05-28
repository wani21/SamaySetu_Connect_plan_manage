-- ============================================================================
-- SHORT NAME FEATURE - FIXED DATABASE MIGRATION SCRIPT
-- ============================================================================
-- Purpose: Add short_name field with proper validation and error handling
-- Date: May 28, 2026
-- Author: Abhijeet Rane
-- ============================================================================

-- Step 1: Add short_name column (nullable initially)
-- ============================================================================
ALTER TABLE users ADD COLUMN IF NOT EXISTS short_name VARCHAR(5);

-- Step 2: Generate short names with proper validation
-- ============================================================================
-- Strategy: Extract first letter of each word, ensure 2-5 letters

UPDATE users
SET short_name = (
    SELECT 
        CASE 
            -- If generated initials are less than 2 chars, pad with first name letters
            WHEN LENGTH(initials) < 2 THEN 
                UPPER(SUBSTRING(REGEXP_REPLACE(name, '[^a-zA-Z]', '', 'g'), 1, 2))
            -- If generated initials are more than 5 chars, take first 5
            WHEN LENGTH(initials) > 5 THEN 
                SUBSTRING(initials, 1, 5)
            -- Otherwise use the initials as-is
            ELSE initials
        END
    FROM (
        SELECT STRING_AGG(SUBSTRING(part, 1, 1), '') as initials
        FROM UNNEST(STRING_TO_ARRAY(TRIM(UPPER(REGEXP_REPLACE(name, '[^a-zA-Z ]', '', 'g'))), ' ')) AS part
        WHERE part != ''
    ) sub
)
WHERE short_name IS NULL;

-- Step 3: Handle any remaining NULL or invalid short names
-- ============================================================================
-- Fallback: Use first 3 letters of name if still NULL or invalid

UPDATE users
SET short_name = UPPER(SUBSTRING(REGEXP_REPLACE(name, '[^a-zA-Z]', '', 'g'), 1, 3))
WHERE short_name IS NULL 
   OR short_name = '' 
   OR LENGTH(short_name) < 2 
   OR LENGTH(short_name) > 5
   OR short_name !~ '^[A-Z]+$';

-- Step 4: Verify all short names are valid before adding constraint
-- ============================================================================
SELECT 
    id,
    name,
    short_name,
    LENGTH(short_name) as length,
    CASE 
        WHEN short_name IS NULL THEN '✗ NULL'
        WHEN LENGTH(short_name) < 2 THEN '✗ Too Short'
        WHEN LENGTH(short_name) > 5 THEN '✗ Too Long'
        WHEN short_name !~ '^[A-Z]+$' THEN '✗ Invalid Format'
        ELSE '✓ Valid'
    END as validation_status
FROM users
WHERE short_name IS NULL 
   OR LENGTH(short_name) < 2 
   OR LENGTH(short_name) > 5 
   OR short_name !~ '^[A-Z]+$';

-- If above query returns any rows, manually fix them:
-- UPDATE users SET short_name = 'XXX' WHERE id = Y;

-- Step 5: Handle duplicate short names with alphabetic suffixes
-- ============================================================================
WITH duplicates AS (
    SELECT 
        short_name,
        id,
        ROW_NUMBER() OVER (PARTITION BY short_name ORDER BY id) as rn
    FROM users
    WHERE short_name IS NOT NULL
),
suffix_map AS (
    SELECT 
        rn,
        CASE 
            WHEN rn = 1 THEN ''
            WHEN rn = 2 THEN 'A'
            WHEN rn = 3 THEN 'B'
            WHEN rn = 4 THEN 'C'
            WHEN rn = 5 THEN 'D'
            WHEN rn = 6 THEN 'E'
            WHEN rn = 7 THEN 'F'
            WHEN rn = 8 THEN 'G'
            WHEN rn = 9 THEN 'H'
            WHEN rn = 10 THEN 'I'
            WHEN rn = 11 THEN 'J'
            WHEN rn = 12 THEN 'K'
            WHEN rn = 13 THEN 'L'
            WHEN rn = 14 THEN 'M'
            WHEN rn = 15 THEN 'N'
            WHEN rn = 16 THEN 'O'
            WHEN rn = 17 THEN 'P'
            WHEN rn = 18 THEN 'Q'
            WHEN rn = 19 THEN 'R'
            WHEN rn = 20 THEN 'S'
            WHEN rn = 21 THEN 'T'
            WHEN rn = 22 THEN 'U'
            WHEN rn = 23 THEN 'V'
            WHEN rn = 24 THEN 'W'
            WHEN rn = 25 THEN 'X'
            WHEN rn = 26 THEN 'Y'
            ELSE 'Z'
        END as suffix
    FROM generate_series(1, 27) as rn
)
UPDATE users u
SET short_name = SUBSTRING(d.short_name || sm.suffix, 1, 5)
FROM duplicates d
INNER JOIN suffix_map sm ON sm.rn = d.rn
WHERE u.id = d.id 
  AND d.rn > 1;

-- Step 6: Final validation check
-- ============================================================================
SELECT 
    short_name, 
    COUNT(*) as count,
    STRING_AGG(name, ', ') as teachers
FROM users 
GROUP BY short_name 
HAVING COUNT(*) > 1;

-- If duplicates still exist, manually resolve:
-- UPDATE users SET short_name = 'NEWVAL' WHERE id = X;

-- Step 7: Verify all short names are valid format
-- ============================================================================
SELECT 
    id,
    name,
    short_name,
    LENGTH(short_name) as length,
    short_name ~ '^[A-Z]{2,5}$' as is_valid
FROM users
WHERE short_name !~ '^[A-Z]{2,5}$'
   OR short_name IS NULL;

-- If any invalid rows exist, fix them manually before proceeding

-- Step 8: Add constraints (only after all validations pass)
-- ============================================================================

-- Make column NOT NULL
ALTER TABLE users ALTER COLUMN short_name SET NOT NULL;

-- Add unique constraint
ALTER TABLE users ADD CONSTRAINT unique_short_name UNIQUE (short_name);

-- Add check constraint for format validation
ALTER TABLE users ADD CONSTRAINT check_short_name_format 
    CHECK (short_name ~ '^[A-Z]{2,5}$');

-- Step 9: Create index for performance
-- ============================================================================
CREATE INDEX IF NOT EXISTS idx_users_short_name ON users(short_name);

-- Step 10: Final verification
-- ============================================================================
SELECT 
    COUNT(*) as total_users,
    COUNT(DISTINCT short_name) as unique_short_names,
    MIN(LENGTH(short_name)) as min_length,
    MAX(LENGTH(short_name)) as max_length,
    ROUND(AVG(LENGTH(short_name))::NUMERIC, 2) as avg_length
FROM users;

-- Show all users with their short names
SELECT 
    id,
    name,
    short_name,
    email,
    employee_id
FROM users 
ORDER BY id;

-- ============================================================================
-- ROLLBACK SCRIPT (if needed)
-- ============================================================================
-- ALTER TABLE users DROP CONSTRAINT IF EXISTS check_short_name_format;
-- ALTER TABLE users DROP CONSTRAINT IF EXISTS unique_short_name;
-- DROP INDEX IF EXISTS idx_users_short_name;
-- ALTER TABLE users ALTER COLUMN short_name DROP NOT NULL;
-- ALTER TABLE users DROP COLUMN IF EXISTS short_name;

-- ============================================================================
-- MANUAL FIX EXAMPLES
-- ============================================================================
-- If you need to manually fix specific short names:

-- Example 1: Single character name (e.g., "A")
-- UPDATE users SET short_name = 'AA' WHERE name = 'A';

-- Example 2: Name with special characters (e.g., "Dr. John")
-- UPDATE users SET short_name = 'DRJ' WHERE name = 'Dr. John';

-- Example 3: Very long name generating >5 char initials
-- UPDATE users SET short_name = SUBSTRING(short_name, 1, 5) WHERE LENGTH(short_name) > 5;

-- ============================================================================
-- END OF MIGRATION SCRIPT
-- ============================================================================
