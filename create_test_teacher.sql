-- Create a test teacher for authentication
-- First, you need to get the hashed password from POST http://localhost:8083/auth/ endpoint

USE samaysetu;

-- Insert test teacher (replace YOUR_HASHED_PASSWORD with the actual hash from the endpoint)
INSERT INTO teachers (name, employee_id, email, phone, weekly_hours_limit, specialization, is_active, password, role, created_at, updated_at)
VALUES (
    'Test Teacher',
    'EMP001',
    'teacher@test.com',
    '1234567890',
    25,
    'Computer Science',
    1,
    '$2a$10$YOUR_HASHED_PASSWORD_HERE',  -- Replace this with actual hash
    'TEACHER',
    NOW(),
    NOW()
);

-- Verify the teacher was created
SELECT id, name, email, role FROM teachers;
