# Teacher Approval Workflow - Complete Implementation Guide

## ✅ Backend Implementation (COMPLETED)

### 1. Database Migration
Run this SQL script:
```sql
-- File: add_teacher_approval_status.sql
ALTER TABLE teachers 
ADD COLUMN is_approved TINYINT(1) DEFAULT 0 AFTER is_active;

UPDATE teachers 
SET is_approved = 1 
WHERE is_active = 1;
```

### 2. Entity Updated
- `TeacherEntity.java` - Added `isApproved` field ✅

### 3. Repository Updated
- `Teacher_Repo.java` - Added `findByIsApprovedAndIsEmailVerified()` ✅

### 4. Service Updated
- `TeacherService.java` - Added approval methods ✅
  - `getPendingApprovals()`
  - `approveTeacher(Long id)`
  - `rejectTeacher(Long id, String reason)`

### 5. Controller Updated
- `TeacherController.java` - Added endpoints ✅
  - `GET /api/teachers/pending-approvals`
  - `POST /api/teachers/{id}/approve`
  - `POST /api/teachers/{id}/reject`

### 6. Email Service Updated
- `EmailService.java` - Added email methods ✅
  - `sendApprovalEmail()`
  - `sendRejectionEmail()`

## ✅ Frontend API (COMPLETED)

### api.ts
```typescript
export const teacherAdminAPI = {
  getAll: () => api.get('/api/teachers'),
  getPendingApprovals: () => api.get('/api/teachers/pending-approvals'),
  approve: (id: number) => api.post(`/api/teachers/${id}/approve`),
  reject: (id: number, reason?: string) => api.post(`/api/teachers/${id}/reject`, reason),
  // ... other methods
};
```

## 🔧 Frontend UI Implementation (IN PROGRESS)

### TeachersPageComplete.tsx - Required Changes

#### 1. Add State Variables
```typescript
const [pendingTeachers, setPendingTeachers] = useState<any[]>([]);
const [activeTab, setActiveTab] = useState<'approved' | 'pending'>('approved');
```

#### 2. Add Fetch Methods
```typescript
const fetchPendingApprovals = async () => {
  try {
    const response = await teacherAdminAPI.getPendingApprovals();
    setPendingTeachers(Array.isArray(response.data) ? response.data : []);
  } catch (error: any) {
    console.error('Failed to fetch pending approvals:', error);
    setPendingTeachers([]);
  }
};
```

#### 3. Add Approval Handlers
```typescript
const handleApprove = async (id: number, name: string) => {
  if (!window.confirm(`Approve ${name}'s account?`)) return;
  
  try {
    await teacherAdminAPI.approve(id);
    toast.success(`${name} approved successfully!`);
    fetchTeachers();
    fetchPendingApprovals();
  } catch (error: any) {
    const errorMessage = getErrorMessage(error);
    toast.error(errorMessage, { duration: 5000 });
  }
};

const handleReject = async (id: number, name: string) => {
  const reason = window.prompt(`Reject ${name}'s account?\n\nOptional: Enter reason:`);
  if (reason === null) return;
  
  try {
    await teacherAdminAPI.reject(id, reason || 'Application rejected');
    toast.success(`${name}'s application rejected`);
    fetchPendingApprovals();
  } catch (error: any) {
    const errorMessage = getErrorMessage(error);
    toast.error(errorMessage, { duration: 5000 });
  }
};
```

#### 4. Update useEffect
```typescript
useEffect(() => {
  fetchTeachers();
  fetchPendingApprovals();
}, []);
```

#### 5. Add Tabs UI (in return statement)
```tsx
{/* Tabs */}
<div className="flex gap-4 mb-6 border-b border-gray-200">
  <button
    onClick={() => setActiveTab('approved')}
    className={`pb-3 px-4 font-medium ${
      activeTab === 'approved'
        ? 'text-primary-600 border-b-2 border-primary-600'
        : 'text-gray-600'
    }`}
  >
    Approved Teachers ({teachers.length})
  </button>
  <button
    onClick={() => setActiveTab('pending')}
    className={`pb-3 px-4 font-medium relative ${
      activeTab === 'pending'
        ? 'text-primary-600 border-b-2 border-primary-600'
        : 'text-gray-600'
    }`}
  >
    Pending Approvals
    {pendingTeachers.length > 0 && (
      <span className="ml-2 px-2 py-0.5 bg-orange-500 text-white text-xs rounded-full">
        {pendingTeachers.length}
      </span>
    )}
  </button>
</div>
```

#### 6. Add Pending Approvals Cards
```tsx
{activeTab === 'pending' && (
  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
    {pendingTeachers.map((teacher) => (
      <Card key={teacher.id} className="border-l-4 border-orange-500">
        <div className="space-y-3">
          <div>
            <h3 className="text-lg font-bold">{teacher.name}</h3>
            <span className="px-2 py-1 bg-orange-100 text-orange-800 text-xs rounded-full">
              ⏳ Pending Approval
            </span>
          </div>
          
          <div className="space-y-2 text-sm text-gray-600">
            <div>Employee ID: {teacher.employeeId}</div>
            <div>Email: {teacher.email}</div>
            {teacher.specialization && <div>Specialization: {teacher.specialization}</div>}
          </div>

          <div className="flex gap-2 pt-3 border-t">
            <Button
              variant="primary"
              onClick={() => handleApprove(teacher.id, teacher.name)}
              className="flex-1"
            >
              ✓ Approve
            </Button>
            <Button
              variant="outline"
              onClick={() => handleReject(teacher.id, teacher.name)}
              className="flex-1 text-red-600 border-red-600"
            >
              ✗ Reject
            </Button>
          </div>
        </div>
      </Card>
    ))}
  </div>
)}
```

## 🔐 Login Error Messages (COMPLETED)

### TeacherService.java - loadUserByUsername()
```java
// Check if email is verified
if (!teach.getIsEmailVerified()) {
    throw new RuntimeException("Email not verified. Please check your email for verification link.");
}

// Check if approved by admin
if (!teach.getIsApproved()) {
    throw new RuntimeException("Your account is pending admin approval. Please wait for approval.");
}

// Check if account is active
if (!teach.getIsActive()) {
    throw new RuntimeException("Account is not active. Please contact administrator.");
}
```

These error messages will be shown to teachers when they try to login:
- **Not verified**: "Email not verified. Please check your email for verification link."
- **Not approved**: "Your account is pending admin approval. Please wait for approval." ✅
- **Not active**: "Account is not active. Please contact administrator."

## 📧 Email Flow

### 1. Registration
- Teacher registers → Receives verification email

### 2. Email Verification
- Teacher clicks link → Email verified
- Message: "Email verified! Your account is pending admin approval."

### 3. Admin Approval
- Admin approves → Teacher receives approval email
- Email content: "Your account has been approved! You can now login."

### 4. Admin Rejection
- Admin rejects → Teacher receives rejection email
- Email content: "Your application has not been approved. Reason: [reason]"

## 🧪 Testing Steps

1. **Register New Teacher**
   - Go to /register
   - Fill form with @mitaoe.ac.in email
   - Submit → Should see "Check your email"

2. **Verify Email**
   - Click verification link in email
   - Should see success message
   - Try to login → Should see "pending admin approval" message ✅

3. **Admin Views Pending**
   - Login as admin
   - Go to Teachers page
   - Click "Pending Approvals" tab
   - Should see new teacher with badge

4. **Admin Approves**
   - Click "Approve" button
   - Confirm approval
   - Teacher should receive approval email

5. **Teacher Logs In**
   - Teacher tries to login
   - Should succeed and access dashboard

6. **Admin Rejects** (Alternative Flow)
   - Click "Reject" button
   - Enter reason (optional)
   - Teacher receives rejection email
   - Teacher cannot login

## 📝 Next Steps

1. Fix TeachersPageComplete.tsx syntax errors from autofix
2. Add the tabs UI
3. Add the pending approvals cards
4. Test the complete workflow
5. Restart backend to apply all changes
6. Run SQL migration script

## 🎯 Benefits

- ✅ Secure registration process
- ✅ Admin control over teacher accounts
- ✅ Clear status messages for teachers
- ✅ Email notifications at each step
- ✅ Backup manual "Add Teacher" option
- ✅ Professional approval workflow
