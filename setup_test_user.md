# Setup Test User for JWT Authentication

## Step 1: Generate BCrypt Password Hash

**Postman Request:**
```
POST http://localhost:8083/auth/
Content-Type: application/json

Body:
{
  "password": "test123"
}
```

**Example Response:**
```
$2a$10$N9qo8uLOickgx2ZMRZoMyeIH9QZXqb5UzVYhYXqLqLqLqLqLqLqLq
```

Copy this hash!

---

## Step 2: Insert Teacher into Database

Open MySQL command line or MySQL Workbench and run:

```sql
USE samaysetu;

-- Delete any existing test user first
DELETE FROM teachers WHERE email = 'teacher@test.com';

-- Insert new teacher with the BCrypt hash you copied
INSERT INTO teachers (
    name, 
    employee_id, 
    email, 
    phone, 
    weekly_hours_limit, 
    specialization, 
    is_active, 
    password, 
    role, 
    created_at, 
    updated_at
)
VALUES (
    'Test Teacher',
    'EMP001',
    'teacher@test.com',
    '1234567890',
    25,
    'Computer Science',
    1,
    'PASTE_YOUR_BCRYPT_HASH_HERE',  -- Replace with actual hash from Step 1
    'TEACHER',
    NOW(),
    NOW()
);

-- Verify the teacher was created
SELECT id, name, email, role, password FROM teachers WHERE email = 'teacher@test.com';
```

---

## Step 3: Test Login

**Postman Request:**
```
POST http://localhost:8083/auth/login
Content-Type: application/json

Body:
{
  "email": "teacher@test.com",
  "password": "test123"
}
```

**Expected Success Response:**
```json
{
  "email": "teacher@test.com",
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZWFjaGVyQHRlc3QuY29tIiwiaWF0IjoxNjk...",
  "role": "TEACHER"
}
```

---

## Step 4: Test Protected Endpoint

Copy the token from Step 3, then:

**Postman Request:**
```
GET http://localhost:8083/api/teachers
Authorization: Bearer YOUR_TOKEN_HERE
```

In Postman:
1. Go to **Authorization** tab
2. Select **Type: Bearer Token**
3. Paste your token
4. Send request

**Expected:** 200 OK with teachers data

---

## Quick MySQL Command Line

```bash
mysql -u root -proot samaysetu
```

Then paste the INSERT statement with your BCrypt hash.

---

## Alternative: Create Admin User

For admin access, use role 'ADMIN':

```sql
INSERT INTO teachers (
    name, employee_id, email, phone, weekly_hours_limit, 
    specialization, is_active, password, role, created_at, updated_at
)
VALUES (
    'Admin User',
    'ADMIN001',
    'admin@test.com',
    '9876543210',
    40,
    'Administration',
    1,
    'YOUR_BCRYPT_HASH_HERE',
    'ADMIN',
    NOW(),
    NOW()
);
```

Then login with:
```json
{
  "email": "admin@test.com",
  "password": "test123"
}
```
