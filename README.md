# SamaySetu — College Timetable Management System

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.3-blue.svg)](https://reactjs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.x-blue.svg)](https://www.typescriptlang.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-336791.svg)](https://www.postgresql.org/)

An enterprise-grade timetable management platform built for MIT Academy of Engineering (MITAOE), Pune. Handles academic structure management, staff onboarding, manual timetable building with conflict detection, lab session scheduling, and role-based access for Admin, HOD, Timetable Coordinator, and Teacher roles.

---

## Features

### Timetable Builder
- **Drag-and-drop grid** — visually move and swap entries between cells
- **8-point conflict detection** — teacher, room, division, capacity, availability, break protection, weekly hours (duration-based), room-course type matching
- **Lab session wizard** — single-step creation with auto-consecutive-slot booking, batch-level teacher+room assignment, break-between-slots rejection
- **Semester filter** — view/edit entries by semester within a division
- **Copy timetable** — clone draft entries from one division to another
- **Pre-publish validation** — dashboard with blocking errors and informational warnings before going live

### Role-Based Access Control
| Feature | Admin | HOD | Timetable Coordinator | Teacher |
|---------|:-----:|:---:|:---------------------:|:-------:|
| Dashboard (admin layout) | Yes | Yes | Yes | - |
| Academic Structure | Yes | Yes | Yes | - |
| Staff Management | Yes | Yes | - | - |
| Approve/Reject Teachers | Yes | Yes | - | - |
| Rooms | Yes | Yes | Yes | - |
| Time Slots | Yes | - | Yes | - |
| Timetable Builder | Yes | Yes | Yes | - |
| View Own Timetable | - | - | - | Yes |
| Set Availability | - | - | - | Yes |
| Profile / Password | Yes | Yes | Yes | Yes |

### Export
- **PDF export** — division and teacher timetables with college header, break rows, lab badges
- **Excel export** — styled .xlsx with merged break rows, purple lab cells, 3-line cell content

### Security
- JWT authentication with 1-hour expiry
- Account lockout after 5 failed login attempts (15-minute lock)
- Global XSS input sanitization via `@RestControllerAdvice`
- Tiered rate limiting: auth 10/min, writes 30/min, reads 60/min
- Password strength: 8+ chars, uppercase, lowercase, digit, special character
- Sensitive data removed from logs and API responses
- HSTS, CSP, X-Frame-Options, Cache-Control headers
- Audit logging for admin actions (`[AUDIT]` structured logs)

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| **Backend** | Java 17, Spring Boot 3.5.5, Spring Security, Spring Data JPA |
| **Frontend** | React 18, TypeScript, Tailwind CSS, Zustand, @dnd-kit |
| **Database** | PostgreSQL 17 (localhost dev / AWS RDS prod) |
| **Cache** | Redis (Memurai on Windows) |
| **PDF/Excel** | OpenPDF 1.3.35, Apache POI 5.2.5 |
| **Auth** | JWT (jjwt 0.12.6), BCrypt |
| **CI/CD** | GitHub Actions (build+test auto, deploy manual) |
| **Deployment** | AWS EC2 Auto Scaling + ALB (backend), AWS Amplify (frontend) |

---

## Getting Started

### Prerequisites

- Java 17+
- Node.js 18+ with npm
- PostgreSQL 17 (local)
- Redis or Memurai (local)
- Maven 3.6+

### 1. Clone

```bash
git clone https://github.com/wani21/SamaySetu_Connect_plan_manage.git
cd SamaySetu_Connect_plan_manage
```

### 2. Database Setup

Create a PostgreSQL database named `SamaySetu`:

```bash
createdb -U postgres SamaySetu
```

Start the backend once to let Hibernate create tables (`ddl-auto=update`), then seed development data:

```bash
psql -U postgres -d SamaySetu -f Scripts/seed_data.sql
```

### 3. Backend

```bash
cd Backend
mvn spring-boot:run
```

Runs on `http://localhost:8083`. Uses `application-dev.properties` by default.

### 4. Frontend

```bash
cd Frontend
npm install
npm run dev
```

Runs on `http://localhost:5173`.

---

## Project Structure

```
Backend/
  src/main/java/com/College/timetable/
    Configuration/     SecurityConfig, RedisConfig, InputSanitizationAdvice
    Controller/        REST endpoints (Auth, Admin, Timetable, Staff, etc.)
    Entity/            JPA entities (TeacherEntity, TimetableEntry, Division, etc.)
    Filter/            JWTRequestFilter, RateLimitFilter
    IO/                DTOs (request/response objects)
    Repository/        Spring Data JPA repositories
    Service/           Business logic (TimetableService, ConflictCheckService, etc.)
    Util/              JWTUtil, DataInitializer

Frontend/
  src/
    components/
      admin/           Admin pages (TimetableManagementPage, StaffManagementPage, etc.)
      auth/            ProtectedRoute
      common/          Card, Button, Modal, Input, Loading, ErrorBoundary
      layout/          Navbar, Sidebar
    pages/             App-level pages (Login, Register, Dashboard, etc.)
      teacher/         Teacher pages (TimetablePage, AvailabilityPage, ProfilePage)
    services/          api.ts (all API calls)
    store/             authStore.ts (Zustand)
    types/             TypeScript interfaces

Scripts/
  seed_data.sql        Development seed data (departments, courses, teachers, rooms)
```

---

## Environment Configuration

### Dev vs Prod

| Setting | Dev | Prod |
|---------|-----|------|
| Profile | `SPRING_PROFILES_ACTIVE=dev` | `SPRING_PROFILES_ACTIVE=prod` |
| Database | `localhost:5432/SamaySetu` | `${SPRING_DATASOURCE_URL}` (env var, no default) |
| JWT Secret | Hardcoded dev key | `${JWT_SECRET_KEY}` (env var, no default) |
| ddl-auto | `update` | `validate` |
| Staff password | `mitaoe@123` (fixed for testing) | `random` (UUID-based) |
| Rate limit | 20 req/min (auth) | 10 req/min (auth) |
| SQL logging | DEBUG | WARN |

### Required Environment Variables (Production)

```bash
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/samaysetu
SPRING_DATASOURCE_USERNAME=...
SPRING_DATASOURCE_PASSWORD=...
JWT_SECRET_KEY=<base64-encoded-256bit-key>
EMAIL_USERNAME=...
EMAIL_PASSWORD=...
APP_BASE_URL=https://api.yourdomain.com
APP_FRONTEND_URL=https://your-app.amplifyapp.com
APP_CORS_ALLOWED_ORIGINS=https://your-app.amplifyapp.com
APP_ADMIN_EMAIL=admin@mitaoe.ac.in
APP_ADMIN_PASSWORD=<strong-password>
REDIS_HOST=your-redis-host
REDIS_PORT=6379
REDIS_PASSWORD=...
```

Frontend production env var (set in AWS Amplify Console, NOT in `.env.production`):

```
VITE_API_URL=https://api.yourdomain.com
```

---

## CI/CD

| Workflow | Trigger | Purpose |
|----------|---------|---------|
| `ci.yml` | Auto on push/PR to `main` + feature branches | Build + test (always runs) |
| `deploy-backend.yml` | Manual (`workflow_dispatch`) | Full AWS deployment (enable auto-deploy when AWS is configured) |

---

## API Endpoints

### Public
```
POST /auth/login                         Login
POST /auth/register                      Registration (college email only)
GET  /auth/verify-email?token=           Email verification
POST /auth/forgot-password               Password reset request
POST /auth/reset-password                Password reset
POST /auth/change-first-password         First login password change
```

### Authenticated (any role)
```
GET  /api/academic-years                 List academic years
GET  /api/academic-years/current         Current academic year
GET  /api/time-slots                     List time slots
GET  /api/timetable/division/{id}        Published division timetable
GET  /api/timetable/teacher/{id}         Published teacher timetable
GET  /api/staff/profile                  Own profile
PUT  /api/staff/profile                  Update profile
POST /api/staff/change-password          Change password
GET  /api/staff/availability             Get own availability
PUT  /api/staff/availability             Save availability
```

### Admin / HOD / Timetable Coordinator
```
GET    /admin/api/departments            List departments
GET    /admin/api/divisions              List divisions
GET    /admin/api/courses                List courses
GET    /admin/api/rooms                  List rooms
GET    /admin/api/time-slots             List time slots (admin view)
GET    /admin/api/batches                List batches
POST   /api/timetable/entries            Create timetable entry
PUT    /api/timetable/entries/{id}       Update entry (also used by drag-and-drop)
DELETE /api/timetable/entries/{id}       Delete entry
POST   /api/timetable/lab-session        Create lab session (wizard)
DELETE /api/timetable/lab-groups/{id}    Delete lab group
GET    /api/timetable/validate           Pre-publish validation
POST   /api/timetable/publish            Publish timetable
POST   /api/timetable/archive            Archive timetable
POST   /api/timetable/copy               Copy between divisions
GET    /api/timetable/export/division/{id}/pdf    Export PDF
GET    /api/timetable/export/division/{id}/excel  Export Excel
GET    /api/timetable/export/teacher/{id}/pdf     Teacher PDF
```

### Admin + HOD only
```
POST   /admin/upload-staff               CSV staff upload
POST   /admin/create-staff               Manual staff creation
GET    /api/teachers/pending-approvals    Pending teacher approvals
POST   /api/teachers/{id}/approve        Approve teacher
POST   /api/teachers/{id}/reject         Reject teacher
```

---

## Seed Data

The `Scripts/seed_data.sql` script creates realistic development data for MITAOE:

| Entity | Count | Details |
|--------|-------|---------|
| Academic Years | 2 | 2024-25 (past), 2025-26 (current) |
| Departments | 8 | COMP, ENTC, MECH, CIVIL, AIDS (2025-26) + 3 historical |
| Divisions | 10 | COMP SY-A/B, TY-A/B, BTech-A; ENTC, MECH, AIDS |
| Courses | 32 | DSA, OOP, DBMS, ML, Web Dev, Signals, Thermo, etc. |
| Classrooms | 16 | A-wing lecture halls, B-wing comp labs, C-wing ENTC labs, D-wing workshop |
| Time Slots | 10 | 7 periods + 3 breaks (TYPE_1 and TYPE_2) |
| Teachers | 21 | Maharashtra names with department assignments |
| Batches | 12 | 3 per division for lab groups |
| Students | 10 | Sample students in COMP SY |
| Teacher-Course | 32 | Course assignments |

---

## License

This project is licensed under the MIT License.

---

Built for MIT Academy of Engineering, Alandi, Pune, Maharashtra.
