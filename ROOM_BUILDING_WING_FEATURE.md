# Room Building/Wing Feature

## Overview
Added building/wing field to room management to better organize rooms by building or wing location.

## Changes Made

### 1. Backend - Entity Update

**File:** `Backend/src/main/java/com/College/timetable/Entity/ClassRoom.java`

Added new field:
```java
@Size(max = 10)
@Column(name = "building_wing")
private String buildingWing;
```

### 2. Database Migration

**File:** `add_building_wing_to_rooms.sql`

SQL script to add the column:
```sql
ALTER TABLE rooms 
ADD COLUMN building_wing VARCHAR(10) AFTER room_number;
```

**To apply:**
```bash
mysql -u your_username -p your_database < add_building_wing_to_rooms.sql
```

### 3. Frontend - Form Updates

**File:** `Frontend/src/components/admin/RoomsPage.tsx`

#### Added to Form State:
```typescript
const [formData, setFormData] = useState({
  name: '',
  roomNumber: '',
  buildingWing: '',  // ✅ New field
  capacity: '',
  roomType: 'classroom',
  departmentId: '',
  hasProjector: false,
  hasAc: false,
  equipment: '',
});
```

#### Room Number Validation:
```typescript
// Validates format: Letter + 3 digits (e.g., H202, A101, B305)
const roomNumberPattern = /^[A-Z]\d{3}$/;
if (formData.roomNumber && !roomNumberPattern.test(formData.roomNumber)) {
  newErrors.roomNumber = 'Room number must be in format: Letter + 3 digits (e.g., H202, A101)';
}
```

#### API Payload:
```typescript
await roomAPI.create({
  name: formData.name,
  roomNumber: formData.roomNumber.toUpperCase(),
  buildingWing: formData.buildingWing || null,  // ✅ New field
  roomType: formData.roomType.toUpperCase(),
  capacity: parseInt(formData.capacity),
  hasProjector: formData.hasProjector,
  hasAc: formData.hasAc,
  equipment: formData.equipment || null,
  department: {
    id: parseInt(formData.departmentId)
  },
});
```

#### Display Updates:
- Room cards now show building/wing next to room number
- Format: "Room #H202 • Wing H"

#### Form Fields:
1. **Room Number** - Auto-converts to uppercase, validates format
2. **Building/Wing** - Optional text field, auto-converts to uppercase

## Features

### Smart Room Number & Wing Synchronization

#### Scenario 1: Enter Room Number First
- **Input:** Room Number = "H202"
- **Result:** Wing automatically set to "H"

#### Scenario 2: Enter Digits Only + Wing
- **Input:** Room Number = "202", Wing = "H"
- **Result:** Room Number automatically becomes "H202"

#### Scenario 3: Change Wing After Room Number
- **Input:** Room Number = "A202", then change Wing to "H"
- **Result:** Room Number automatically updates to "H202"

### Room Number Format
- **Required format:** Letter + 3 digits
- **Examples:** H202, A101, B305, C410
- **Input flexibility:** Can enter "H202" or just "202" (if wing is set)
- **Validation:** Automatic uppercase conversion and format validation
- **Error message:** Shows if format is incorrect or doesn't match wing

### Building/Wing Field
- **Required field** (changed from optional)
- **Purpose:** Identify which building or wing the room is in
- **Examples:** A, B, C, D, H (for Hostel), M (for Main), etc.
- **Max length:** 10 characters
- **Auto-uppercase:** Automatically converts to uppercase
- **Auto-sync:** Syncs with room number in real-time

## UI/UX Improvements

### Form
- Clear placeholder text: "H202" for room number
- Helper text explaining format requirements
- Building/wing field with descriptive placeholder
- Automatic uppercase conversion for consistency

### Display
- Room cards show wing information inline with room number
- Visual separator (•) between room number and wing
- Wing displayed in primary color for emphasis

## Testing Checklist

- ✅ Run database migration script
- ✅ Restart Spring Boot backend
- ✅ Create room with room number H202
- ✅ Create room with building wing "H"
- ✅ Create room without building wing (optional)
- ✅ Verify room number validation (should reject "H20" or "202")
- ✅ Verify room cards display wing information
- ✅ Verify existing rooms still work

## Example Usage

### Creating a Room
```
Room Name: Lecture Hall 202
Room Number: H202
Building/Wing: H
Room Type: Classroom
Capacity: 60
Department: Computer Science
```

### Display Result
```
Lecture Hall 202
Room #H202 • Wing H
```

## Migration Notes

**For existing rooms:**
- The `building_wing` column is nullable
- Existing rooms will have NULL for building_wing
- Can be updated later through edit functionality
- Consider bulk updating based on room number prefix:
  ```sql
  UPDATE rooms SET building_wing = 'H' WHERE room_number LIKE 'H%';
  UPDATE rooms SET building_wing = 'A' WHERE room_number LIKE 'A%';
  ```
