-- Add approval status column to teachers table
-- This enables admin approval workflow for teacher registrations

-- Add is_approved column (default false for new registrations)
ALTER TABLE teachers 
ADD COLUMN is_approved TINYINT(1) DEFAULT 0 AFTER is_active;

-- Update existing teachers to approved status
UPDATE teachers 
SET is_approved = 1 
WHERE is_active = 1;

-- Add comment for clarity
ALTER TABLE teachers 
MODIFY COLUMN is_approved TINYINT(1) DEFAULT 0 COMMENT 'Admin approval status: 0=pending, 1=approved';

-- Verify the changes
SELECT id, name, email, is_email_verified, is_approved, is_active 
FROM teachers 
ORDER BY created_at DESC;
