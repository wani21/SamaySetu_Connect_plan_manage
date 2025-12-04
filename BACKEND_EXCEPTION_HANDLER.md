# Backend Global Exception Handler

## Problem
When duplicate entries or database constraint violations occurred, the backend was returning:
- 403 Forbidden status (incorrect)
- HTML error pages instead of JSON
- No user-friendly error messages

This caused the frontend to show only a red X with no message.

## Solution

Created a global exception handler (`GlobalExceptionHandler.java`) that intercepts all exceptions and returns proper HTTP status codes with JSON error messages.

## Exception Handler Features

### 1. DataIntegrityViolationException (Database Constraints)
**Status Code:** 409 Conflict

**Handles:**
- Duplicate entry errors
- Null constraint violations
- Foreign key violations

**Example Responses:**
```json
{
  "message": "A record with room number 'H202' already exists.",
  "status": "409"
}
```

```json
{
  "message": "department id is required.",
  "status": "409"
}
```

### 2. EntityNotFoundException
**Status Code:** 404 Not Found

**Example Response:**
```json
{
  "message": "Course not found with id: 123",
  "status": "404"
}
```

### 3. MethodArgumentNotValidException (Validation Errors)
**Status Code:** 400 Bad Request

**Example Response:**
```json
{
  "message": "Validation failed",
  "errors": {
    "name": "Room name is required",
    "capacity": "Capacity must be at least 1"
  },
  "status": "400"
}
```

### 4. IllegalArgumentException
**Status Code:** 400 Bad Request

**Example Response:**
```json
{
  "message": "Invalid room type provided",
  "status": "400"
}
```

### 5. Generic Exception (Catch-all)
**Status Code:** 500 Internal Server Error

**Example Response:**
```json
{
  "message": "An unexpected error occurred: ...",
  "status": "500"
}
```

## Frontend Error Handler Updates

Updated `errorHandler.ts` to:
1. Log full error details for debugging
2. Handle HTML error pages gracefully
3. Parse new JSON error format from backend
4. Extract duplicate entry information from 403 errors (fallback)

## Benefits

### Before
- ❌ 403 status for duplicate entries
- ❌ HTML error pages
- ❌ No error message shown to user
- ❌ User gets logged out

### After
- ✅ 409 status for duplicate entries
- ✅ JSON error responses
- ✅ Clear error messages: "A record with room number 'H202' already exists."
- ✅ User stays logged in
- ✅ Consistent error handling across all endpoints

## Error Message Examples

### Duplicate Room Number
**Action:** Create room with existing room number "H202"
**Response:**
```json
{
  "message": "A record with room number 'H202' already exists.",
  "status": "409"
}
```
**User Sees:** "A record with room number 'H202' already exists."

### Duplicate Course Code
**Action:** Create course with existing code "CS301"
**Response:**
```json
{
  "message": "A record with code 'CS301' already exists.",
  "status": "409"
}
```
**User Sees:** "A record with code 'CS301' already exists."

### Missing Required Field
**Action:** Create room without capacity
**Response:**
```json
{
  "message": "capacity is required.",
  "status": "409"
}
```
**User Sees:** "capacity is required."

### Validation Error
**Action:** Create room with invalid data
**Response:**
```json
{
  "message": "Validation failed",
  "errors": {
    "capacity": "Capacity must be at least 1",
    "roomNumber": "Room number is required"
  },
  "status": "400"
}
```
**User Sees:** "Capacity must be at least 1, Room number is required"

## Implementation Details

### Exception Handler Location
```
Backend/src/main/java/com/College/timetable/Exception/GlobalExceptionHandler.java
```

### Annotations Used
- `@RestControllerAdvice` - Makes this a global exception handler
- `@ExceptionHandler` - Specifies which exception to handle

### Error Parsing
The handler intelligently parses database error messages:
```java
// From: "Duplicate entry 'H202' for key 'rooms.room_number'"
// To: "A record with room number 'H202' already exists."
```

## Testing

### Test Cases
1. ✅ Create duplicate room → 409 with clear message
2. ✅ Create duplicate course → 409 with clear message
3. ✅ Create duplicate department → 409 with clear message
4. ✅ Missing required field → 409 with field name
5. ✅ Validation error → 400 with field errors
6. ✅ Entity not found → 404 with message
7. ✅ All errors return JSON (not HTML)
8. ✅ User stays logged in on validation errors

### How to Test
1. Restart Spring Boot backend
2. Try to create a room with existing room number
3. Should see: "A record with room number 'H202' already exists."
4. Should NOT get logged out
5. Check browser console for error details

## Future Improvements

1. **Localization**
   - Support multiple languages for error messages
   - Use message bundles

2. **Error Codes**
   - Add unique error codes for each error type
   - Example: `ERR_DUPLICATE_ENTRY_001`

3. **Detailed Validation**
   - Return which specific constraint was violated
   - Suggest corrections

4. **Rate Limiting**
   - Add rate limiting for repeated errors
   - Prevent abuse
