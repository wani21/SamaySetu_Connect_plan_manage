-- ============================================================================
-- BATCH STRENGTH FEATURE - DATABASE MIGRATION SCRIPT
-- ============================================================================
-- Purpose: Add and initialize batch strength field for existing batches
-- Date: May 28, 2026
-- Author: Abhijeet Rane
-- ============================================================================

-- Step 1: Check if strength column exists
-- ============================================================================
SELECT 
    column_name, 
    data_type, 
    is_nullable,
    column_default
FROM information_schema.columns 
WHERE table_name = 'batches' 
  AND column_name = 'strength';

-- If the column doesn't exist (it should based on entity), uncomment below:
-- ALTER TABLE batches ADD COLUMN strength INTEGER;

-- Step 2: Check current state of batches
-- ============================================================================
SELECT 
    b.id AS batch_id,
    b.name AS batch_name,
    b.strength AS current_strength,
    d.id AS division_id,
    d.name AS division_name,
    d.total_students AS division_strength
FROM batches b
INNER JOIN divisions d ON b.division_id = d.id
ORDER BY d.id, b.id;

-- Step 3: Initialize strength for batches with NULL values
-- ============================================================================
-- Strategy: Distribute division strength evenly across batches
-- If division has 60 students and 3 batches, each gets 20 students

UPDATE batches b
SET strength = FLOOR(
    (SELECT d.total_students 
     FROM divisions d 
     WHERE d.id = b.division_id) 
    / 
    NULLIF((SELECT COUNT(*) 
            FROM batches b2 
            WHERE b2.division_id = b.division_id), 0)
)
WHERE strength IS NULL;

-- Step 4: Handle remainder students (assign to last batch in each division)
-- ============================================================================
-- If division has 61 students and 3 batches (20, 20, 20), 
-- the last batch should get the extra 1 student (20, 20, 21)

WITH division_stats AS (
    SELECT 
        d.id AS division_id,
        d.total_students AS division_strength,
        COUNT(b.id) AS batch_count,
        COALESCE(SUM(b.strength), 0) AS allocated_strength,
        d.total_students - COALESCE(SUM(b.strength), 0) AS remainder
    FROM divisions d
    LEFT JOIN batches b ON b.division_id = d.id
    GROUP BY d.id, d.total_students
),
last_batches AS (
    SELECT DISTINCT ON (b.division_id)
        b.id AS batch_id,
        b.division_id,
        b.strength AS current_strength
    FROM batches b
    ORDER BY b.division_id, b.id DESC
)
UPDATE batches
SET strength = batches.strength + ds.remainder
FROM last_batches lb
INNER JOIN division_stats ds ON ds.division_id = lb.division_id
WHERE batches.id = lb.batch_id
  AND ds.remainder > 0;

-- Step 5: Verify the migration
-- ============================================================================
SELECT 
    d.id AS division_id,
    d.name AS division_name,
    d.total_students AS division_strength,
    COUNT(b.id) AS batch_count,
    COALESCE(SUM(b.strength), 0) AS total_batch_strength,
    d.total_students - COALESCE(SUM(b.strength), 0) AS unallocated_students,
    CASE 
        WHEN d.total_students = COALESCE(SUM(b.strength), 0) THEN '✓ PERFECT'
        WHEN d.total_students > COALESCE(SUM(b.strength), 0) THEN '⚠ UNDER-ALLOCATED'
        ELSE '✗ OVER-ALLOCATED'
    END AS status
FROM divisions d
LEFT JOIN batches b ON b.division_id = d.id
GROUP BY d.id, d.name, d.total_students
ORDER BY d.id;

-- Step 6: Detailed batch-level verification
-- ============================================================================
SELECT 
    d.id AS division_id,
    d.name AS division_name,
    d.total_students AS division_strength,
    b.id AS batch_id,
    b.name AS batch_name,
    b.strength AS batch_strength,
    ROUND(
        (b.strength::NUMERIC / NULLIF(d.total_students, 0)) * 100, 
        2
    ) AS percentage_of_division
FROM divisions d
LEFT JOIN batches b ON b.division_id = d.id
ORDER BY d.id, b.id;

-- Step 7: Make strength column NOT NULL (optional - for data integrity)
-- ============================================================================
-- Only run this after ensuring all batches have strength values
-- ALTER TABLE batches ALTER COLUMN strength SET NOT NULL;

-- Step 8: Add check constraint (optional - for validation)
-- ============================================================================
-- Ensure batch strength is always non-negative
-- ALTER TABLE batches ADD CONSTRAINT check_batch_strength_positive 
--     CHECK (strength >= 0);

-- ============================================================================
-- ROLLBACK SCRIPT (if needed)
-- ============================================================================
-- To revert the changes:
-- UPDATE batches SET strength = NULL;
-- ALTER TABLE batches ALTER COLUMN strength DROP NOT NULL;
-- ALTER TABLE batches DROP CONSTRAINT IF EXISTS check_batch_strength_positive;

-- ============================================================================
-- NOTES FOR MANUAL ADJUSTMENT
-- ============================================================================
-- After running this script, you may want to manually adjust batch strengths
-- to match actual student distribution. Use the following query to identify
-- divisions that need manual adjustment:

SELECT 
    d.id AS division_id,
    d.name AS division_name,
    d.total_students AS division_strength,
    STRING_AGG(b.name || ': ' || b.strength, ', ' ORDER BY b.id) AS batch_distribution,
    d.total_students - COALESCE(SUM(b.strength), 0) AS needs_adjustment
FROM divisions d
LEFT JOIN batches b ON b.division_id = d.id
GROUP BY d.id, d.name, d.total_students
HAVING d.total_students - COALESCE(SUM(b.strength), 0) != 0
ORDER BY d.id;

-- ============================================================================
-- END OF MIGRATION SCRIPT
-- ============================================================================
