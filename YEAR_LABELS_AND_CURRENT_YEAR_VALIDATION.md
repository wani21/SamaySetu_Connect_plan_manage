# Year Labels Update & Current Academic Year Validation

## Changes Made

### 1. Updated Year Labels ✅

**Old Labels:**
- FE (First Year)
- SE (Second Year)
- TE (Third Year)
- BE (Final Year)

**New Labels:**
- FY (First Year)
- SY (Second Year)
- TY (Third Year)
- BTech (Final Year)

**Files Modified:**

**Frontend:**
- `Frontend/src/components/admin/DivisionsPage.tsx`
  - Updated `getYearLabel()` function
  - Updated dropdown options in division form
- `Frontend/src/pages/teacher/TimetablePage.tsx`
  - Updated hardcoded timetable data with new year labels

**Documentation:**
- `PHASE1_COMPLETE_GUIDE.md` - Updated year mapping
- `BACKEND_COMPLETE_IMPLEMENTATION.md` - Updated example
- `DEMO_CHECKLIST.md` - Updated division example
- `QUICK_START.md` - Updated division example

### 2. Single Current Academic Year Validation ✅

**Problem:** Admin could set multiple academic years as "current" which causes confusion.

**Solution:** Backend validation ensures only ONE academic year can be marked as current at any time.

#### Backend Changes

**Repository (`Acadamic_repo.java`):**
```java
// Find the current academic year
AcademicYear findByIsCurrent(Boolean isCurrent);

// Count how many academic years are marked as current
long countByIsCurrent(Boolean isCurrent);
```

**Service (`AcadamicService.java`):**

**Create Validation:**
```java
public AcademicYear addAcadamic(AcademicYear aca) {
    // If this academic year is being set as current, check for existing current year
    if (aca.getIsCurrent() != null && aca.getIsCurrent()) {
        AcademicYear existingCurrent = acadamy.findByIsCurrent(true);
        if (existingCurrent != null) {
            throw new IllegalArgumentException(
                "An academic year '" + existingCurrent.getYearName() + 
                "' is already set as current. Please unset it first."
            );
        }
    }
    return acadamy.save(aca);
}
```

**Update Validation:**
```java
public AcademicYear update(Long id, AcademicYear aca) {
    AcademicYear existing = getById(id);
    
    // If trying to set this as current, check if another year is already current
    if (aca.getIsCurrent() != null && aca.getIsCurrent() && !existing.getIsCurrent()) {
        AcademicYear existingCurrent = acadamy.findByIsCurrent(true);
        if (existingCurrent != null && !existingCurrent.getId().equals(id)) {
            throw new IllegalArgumentException(
                "Academic year '" + existingCurrent.getYearName() + 
                "' is already set as current. Please unset it first."
            );
        }
    }
    
    // Update fields...
}
```

#### Frontend Changes

**UI Warning (`AcademicYearsPage.tsx`):**
- Added warning message when "Set as current" checkbox is checked
- Shows: "⚠️ Only one academic year can be current at a time. Setting this as current will require unsetting any existing current year first."

**Error Handling:**
- GlobalExceptionHandler catches `IllegalArgumentException`
- Returns 400 Bad Request with user-friendly message
- Frontend displays error in toast notification

## How It Works

### Scenario 1: Creating New Current Year
1. Admin creates academic year "2025-26" and checks "Set as current"
2. Backend checks if any year is already current
3. If "2024-25" is current → Error: "Academic year '2024-25' is already set as current. Please unset it first."
4. Admin must first edit "2024-25" and uncheck "Set as current"
5. Then can set "2025-26" as current

### Scenario 2: Updating Existing Year to Current
1. Admin edits "2025-26" and checks "Set as current"
2. Backend checks if another year is current
3. If "2024-25" is current → Error shown
4. Admin must unset "2024-25" first

### Scenario 3: Unsetting Current Year
1. Admin edits "2024-25" (currently set as current)
2. Unchecks "Set as current"
3. Saves successfully
4. Now no year is current (or admin can set another)

## User Experience

### Before:
- ❌ Multiple years could be marked as current
- ❌ Confusion about which year is actually active
- ❌ Divisions could reference wrong academic year

### After:
- ✅ Only one year can be current at a time
- ✅ Clear error messages guide admin
- ✅ Warning shown in UI before attempting
- ✅ Consistent data integrity

## Testing Checklist

- [ ] Create new academic year with "Set as current" checked
  - Should succeed if no current year exists
  - Should fail with clear message if current year exists
- [ ] Edit existing year to set as current
  - Should fail if another year is current
  - Should succeed after unsetting other year
- [ ] Unset current year
  - Should succeed and allow setting another year as current
- [ ] Year labels show FY, SY, TY, BTech in divisions
- [ ] Error messages are user-friendly and actionable

## Files Modified

### Backend:
- `Backend/src/main/java/com/College/timetable/Repository/Acadamic_repo.java`
- `Backend/src/main/java/com/College/timetable/Service/AcadamicService.java`
- `Backend/src/main/java/com/College/timetable/Entity/AcademicYear.java` (circular reference fix)

### Frontend:
- `Frontend/src/components/admin/DivisionsPage.tsx` (year labels)
- `Frontend/src/components/admin/AcademicYearsPage.tsx` (UI warning)

## Notes

- Backend validation is the primary enforcement mechanism
- Frontend warning is for better UX (prevents unnecessary API calls)
- Error messages include the name of the conflicting academic year
- Validation works for both create and update operations
