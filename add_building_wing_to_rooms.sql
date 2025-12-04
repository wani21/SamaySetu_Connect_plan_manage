-- Add building_wing column to rooms table
ALTER TABLE rooms 
ADD COLUMN building_wing VARCHAR(10) AFTER room_number;

-- Update existing rooms by extracting first letter from room_number
UPDATE rooms 
SET building_wing = LEFT(room_number, 1) 
WHERE building_wing IS NULL AND room_number REGEXP '^[A-Z]';

-- For rooms that don't start with a letter, set a default
UPDATE rooms 
SET building_wing = 'A' 
WHERE building_wing IS NULL;

-- Now make the column NOT NULL
ALTER TABLE rooms 
MODIFY COLUMN building_wing VARCHAR(10) NOT NULL;
