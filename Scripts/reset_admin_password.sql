-- Reset Admin Password Script
-- This script resets the admin password to: admin123
-- BCrypt hash: $2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi

-- Show current admin user
SELECT id, name, email, role, is_active, is_email_verified 
FROM teachers 
WHERE role = 'ADMIN';

-- Update admin password to: admin123
UPDATE teachers 
SET password = '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
    is_email_verified = true,
    is_active = true
WHERE email = 'admin@mitaoe.ac.in';

-- Verify the update
SELECT id, name, email, role, is_active, is_email_verified 
FROM teachers 
WHERE email = 'admin@mitaoe.ac.in';

-- Alternative: If you want to reset by employee ID
-- UPDATE teachers 
-- SET password = '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi'
-- WHERE employee_id = 'ADMIN001';

-- Alternative: Reset all admin users (use with caution)
-- UPDATE teachers 
-- SET password = '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi'
-- WHERE role = 'ADMIN';

-- Common password hashes for reference:
-- admin123:     $2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi
-- password123:  $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
-- mitaoe@123:   $2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr/EpNm1B0JJZ.6Fu
-- test123:      $2a$10$slYQmyNdGzTn7ZLBXBChFOC9f6kFjAqPhccnP6DxlWXx2lPk1C3G6

-- After running this script, you can login with:
-- Email: admin@mitaoe.ac.in
-- Password: admin123
