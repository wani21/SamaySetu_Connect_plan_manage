# Teacher Approval Workflow Implementation

## Overview
Implemented a comprehensive teacher registration and approval workflow where teachers must:
1. Register with college email
2. Verify their email
3. Wait for admin approval
4. Get activated after approval

## Workflow Steps

### Step 1: Teacher Registration
**Action:** Teacher fills registration form
**Process:**
- Teacher provides: name, employee ID, email (@mitaoe.ac.in), phone, specialization, password
- System creates teacher account with:
  - `isEmailVerified = false`
  - `isApproved = false`
  - `isActive = false`
- Verification email sent with 24-hour token

### Step 2: Email Verification
**Action:** Teacher clicks verification link in email
**Process:**
- System verifies token
- Sets `isEmailVerified = true`
- Keeps `isApproved = false` (waiting for admin)
- Keeps `isActive = false` (waiting for admin)
- Teacher sees message: "Email verified! Your account is pending admin approval."

### Step 3: Admin Reviews Pending Requests
**Action:** Admin views "Pending Approvals" tab in Teachers page
**Process:**
- Admin sees list of teachers with:
  - ✅ Email verified
  - ⏳ Pending approval
- Admin can:
  - **Approve**: Activates teacher account
  - **Reject**: Deactivates and sends rejection email

### Step 4: Admin Approval
**Action:** Admin clicks "Approve" button
**Process:**
- Sets `isApproved = true`
- Sets `isActive = true`
- Sends approval email to teacher
- Teacher can now login

### Step 5: Teacher Login
**Action:** Teacher attempts to login
**Validation:**
1. Email must be verified
2. Account must be approved by admin
3. Account must be active

**Error Messages:**
- Not verified: "Email not verified. Please check your email for verification link."
- Not approved: "Your account is pending admin approval. Please wait for approval."
- Not active: "Account is not active. Please contact administrator."

## Database Changes

### New Column: `is_approved`
```sql
ALTER TABLE teachers 
ADD COLUMN is_approved TINYINT(1) DEFAULT 0 AFTER is_active;
```

**Migration Script:** `add_teacher_approval_status.sql`

## Backend Changes

### 1. TeacherEntity.java
```java
@Column(name = "is_approved")
private Boolean isApproved = false;
```

### 2. Teacher_Repo.java
```java
List<TeacherEntity> findByIsApprovedAndIsEmailVerified(Boolean isApproved, Boolean isEmailVerified);
```

### 3. TeacherService.java

**New Methods:**
```java
// Get teachers pending approval (email verified but not approved)
public List<TeacherEntity> getPendingApprovals()

// Approve teacher and activate account
public TeacherEntity approveTeacher(Long id)

// Reject teacher application
public TeacherEntity rejectTeacher(Long id, String reason)
```

**Updated Methods:**
- `register()`: Sets `isApproved = false`
- `verifyEmail()`: Keeps `isActive = false` until admin approves
- `loadUserByUsername()`: Checks `isApproved` before allowing login

### 4. TeacherController.java (To be added)
```java
@GetMapping("/pending-approvals")
public ResponseEntity<List<TeacherEntity>> getPendingApprovals()

@PostMapping("/{id}/approve")
public ResponseEntity<TeacherEntity> approveTeacher(@PathVariable Long id)

@PostMapping("/{id}/reject")
public ResponseEntity<TeacherEntity> rejectTeacher(@PathVariable Long id, @RequestBody String reason)
```

### 5. EmailService.java (To be added)
```java
void sendApprovalEmail(String email, String name)
void sendRejectionEmail(String email, String name, String reason)
```

## Frontend Changes

### 1. TeachersPageComplete.tsx

**Removed:**
- Password field from edit mode (security improvement)

**To Add:**
- "Pending Approvals" tab
- Approve/Reject buttons for pending teachers
- Badge showing approval status
- Filter to show only approved teachers in main list

### 2. API Endpoints (api.ts)
```typescript
export const teacherAdminAPI = {
  getPendingApprovals: () => api.get('/api/teachers/pending-approvals'),
  approve: (id: number) => api.post(`/api/teachers/${id}/approve`),
  reject: (id: number, reason: string) => api.post(`/api/teachers/${id}/reject`, { reason }),
  // ... existing methods
};
```

## UI Design

### Teachers Page Layout
```
┌─────────────────────────────────────────────────┐
│ Teachers                    [+ Add Teacher]     │
├─────────────────────────────────────────────────┤
│ [Approved Teachers] [Pending Approvals (3)]    │
├─────────────────────────────────────────────────┤
│                                                 │
│ Approved Teachers Tab:                          │
│ - Shows only approved teachers                  │
│ - Edit and Delete buttons                       │
│                                                 │
│ Pending Approvals Tab:                          │
│ - Shows teachers with verified email           │
│ - Approve and Reject buttons                    │
│ - Shows registration date                       │
│ - Shows email, employee ID, specialization      │
│                                                 │
└─────────────────────────────────────────────────┘
```

### Pending Approval Card
```
┌──────────────────────────────────────────┐
│ 📧 Dr. John Smith                        │
│ Employee ID: EMP123                      │
│ Email: john.smith@mitaoe.ac.in          │
│ Specialization: Data Structures          │
│ Registered: 2 days ago                   │
│                                          │
│ [✓ Approve] [✗ Reject]                  │
└──────────────────────────────────────────┘
```

## User Experience

### Teacher Journey
1. **Register** → "Registration successful! Check your email to verify."
2. **Verify Email** → "Email verified! Your account is pending admin approval."
3. **Wait** → Cannot login yet
4. **Approved** → Receives approval email
5. **Login** → Can access teacher dashboard

### Admin Journey
1. **View Pending** → See badge with count (e.g., "Pending Approvals (3)")
2. **Review Details** → Check teacher information
3. **Approve** → Teacher gets activated and notified
4. **Reject** → Teacher gets notified with reason

## Security Features

1. **Email Verification Required**: Prevents fake registrations
2. **Admin Approval Required**: Ensures only authorized teachers
3. **College Email Only**: Must use @mitaoe.ac.in domain
4. **Password Not Shown**: Never displayed in edit mode
5. **Token Expiry**: Verification tokens expire in 24 hours

## Benefits

### For Institution
- ✅ Control over who can access the system
- ✅ Verify teacher credentials before activation
- ✅ Audit trail of registrations
- ✅ Prevent unauthorized access

### For Teachers
- ✅ Self-service registration
- ✅ Clear status updates
- ✅ Email notifications at each step
- ✅ No need to contact admin for account creation

### For Admins
- ✅ Centralized approval dashboard
- ✅ Quick approve/reject actions
- ✅ Backup "Add Teacher" button for manual creation
- ✅ Full control over teacher accounts

## Testing Checklist

- [ ] Teacher can register with college email
- [ ] Teacher receives verification email
- [ ] Teacher can verify email via link
- [ ] Teacher cannot login before approval
- [ ] Admin sees pending approval in dashboard
- [ ] Admin can approve teacher
- [ ] Teacher receives approval email
- [ ] Teacher can login after approval
- [ ] Admin can reject teacher
- [ ] Teacher receives rejection email
- [ ] Rejected teacher cannot login
- [ ] Admin can manually add teacher (bypass workflow)
- [ ] Password field hidden in edit mode
- [ ] Only teachers shown (not admins)

## Next Steps

1. Add controller endpoints for approval/rejection
2. Add email templates for approval/rejection
3. Create frontend UI for pending approvals tab
4. Add notification badges
5. Test complete workflow end-to-end

## Files Modified

### Backend:
- `TeacherEntity.java` - Added `isApproved` field
- `Teacher_Repo.java` - Added query method
- `TeacherService.java` - Added approval methods
- `add_teacher_approval_status.sql` - Migration script

### Frontend:
- `TeachersPageComplete.tsx` - Removed password from edit

### To Be Created:
- Controller endpoints for approval
- Email templates
- Frontend pending approvals UI
