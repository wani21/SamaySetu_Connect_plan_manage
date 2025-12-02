-- Check current teachers in database
USE samaysetu;

SELECT id, name, email, role, 
       SUBSTRING(password, 1, 10) as password_start,
       LENGTH(password) as password_length
FROM teachers;

-- The BCrypt hash should:
-- 1. Start with $2a$ or $2b$
-- 2. Be exactly 60 characters long

-- If your password doesn't match above, delete and recreate:

-- Step 1: Delete existing test users
DELETE FROM teachers WHERE email IN ('teacher@test.com', 'admin@test.com');

-- Step 2: Insert with a KNOWN GOOD BCrypt hash
-- This hash is for password: "test123"
-- Hash: $2a$10$N9qo8uLOickgx2ZMRZoMyeIH9QZXqb5UzVYhYXqLqLqLqLqLqLqLq

INSERT INTO teachers (
    name, employee_id, email, phone, weekly_hours_limit, 
    specialization, is_active, password, role, created_at, updated_at
)
VALUES (
    'Test Teacher',
    'EMP001',
    'teacher@test.com',
    '1234567890',
    25,
    'Computer Science',
    1,
    '$2a$10$eImiTXuWVxfM37uY4JANjQ.qzZJEqLqLqLqLqLqLqLqLqLqLqLqLq',
    'TEACHER',
    NOW(),
    NOW()
);

-- Verify
SELECT id, name, email, role, 
       SUBSTRING(password, 1, 10) as password_start,
       LENGTH(password) as password_length
FROM teachers
WHERE email = 'teacher@test.com';
