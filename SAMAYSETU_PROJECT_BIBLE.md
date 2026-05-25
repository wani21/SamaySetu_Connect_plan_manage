# SamaySetu — The Project Bible

**A Complete Technical and Product Reference**

Version: 1.0 · Last updated: 23 April 2026
Branch documented: `main`
Product owner: MIT Academy of Engineering (MITAOE), Alandi, Pune, Maharashtra
Source repository: `SamaySetu_Connect_plan_manage`

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Business Logic & Use Cases](#2-business-logic--use-cases)
3. [System Architecture](#3-system-architecture)
4. [Folder Structure & Codebase Walkthrough](#4-folder-structure--codebase-walkthrough)
5. [Module-by-Module Breakdown](#5-module-by-module-breakdown)
6. [Database Design](#6-database-design)
7. [APIs & Integrations](#7-apis--integrations)
8. [Setup & Installation Guide](#8-setup--installation-guide)
9. [Current Implementation Status](#9-current-implementation-status)
10. [Future Scope & Roadmap](#10-future-scope--roadmap)
11. [Developer Notes & Best Practices](#11-developer-notes--best-practices)
12. [Appendix A — Glossary](#appendix-a--glossary)
13. [Appendix B — End-to-End Walkthrough](#appendix-b--end-to-end-walkthrough-day-in-the-life)
14. [Appendix C — Troubleshooting Guide](#appendix-c--troubleshooting-guide)

---

# 1. Project Overview

## 1.1 What is SamaySetu?

**SamaySetu** is a college-level timetable management web application built for **MIT Academy of Engineering (MITAOE)**, Pune. The name itself tells the story:

- *Samay* = Time (Marathi / Hindi)
- *Setu* = Bridge

"The Bridge of Time" — software that connects students, faculty, rooms, and courses across the fabric of weekly academic scheduling.

The system tagline from the repository description is **"Connect, Plan, Manage"**:

- **Connect** — unify students, teachers, and administrators around one live schedule
- **Plan** — build timetables interactively with drag-and-drop, conflict detection, and validation
- **Manage** — onboard staff, track teacher availability, approve new faculty, publish and archive semesters

At its core, SamaySetu is a full-stack web application with:

- A **Java Spring Boot** backend exposing a REST API (port `8083`)
- A **React + TypeScript** single-page frontend (port `5173` in dev)
- A **PostgreSQL** relational database for all business data
- A **Redis** cache for hot-path reads (published timetables)

## 1.2 The Problem SamaySetu Solves

MITAOE, like most Indian engineering colleges, runs a weekly class schedule (Monday–Saturday) for:

- Multiple **departments** (Computer, ENTC, Mechanical, Civil, AI & Data Science, etc.)
- Multiple **years** (First, Second, Third, Fourth)
- Multiple **divisions** within each year (Division A, B, sometimes C)
- Multiple **semesters** (every division runs semester 1 in odd half, semester 2 in even half, and so on)
- **Theory classes** and **practical labs** that may split a division into 2–3 parallel batches
- Shared resources — **teachers teach multiple divisions**, **rooms are used by multiple departments**, **labs have limited capacity**

Before a system like SamaySetu, this was typically handled on spreadsheets or Word documents. That approach has well-known pains:

| Pain point | Example |
|-----------|---------|
| Double-booking | Prof. Patil is shown in two different classrooms in the same period |
| Room overlap | Two divisions both booked in H202 at 11 am Monday |
| Lab conflict | Lab session scheduled in a period that is actually lunch break |
| Over-allocation | A teacher ends up with 38 hours/week when their limit is 30 |
| Stale copies | Published PDF in the notice board still shows last semester |
| No visibility | Teacher has to hunt through three documents to find "where do I teach Thursday 2 pm?" |
| Manual copy | Every semester someone re-types the whole timetable from scratch |

SamaySetu replaces all of that with:

- A **central database** of academic structure (years, departments, divisions, courses, rooms, time slots, teachers, batches)
- A **visual grid builder** where admins drag and drop entries into cells
- An **8-point conflict detector** that blocks impossible assignments before they are saved
- A **pre-publish validation dashboard** that catches empty days, overworked teachers, and capacity mismatches
- **One-click PDF and Excel export** with college branding
- **Role-based access** — admin builds, teacher reads, student (future) views a clean grid

## 1.3 Target Users

SamaySetu distinguishes four roles. Each has its own view and permissions.

| Role | Who they are | What they do |
|------|--------------|--------------|
| **ADMIN** | The principal's office / IT admin | Full control. Creates academic years, departments, courses, rooms, time slots, staff accounts. Approves new teachers. Builds and publishes every division's timetable. |
| **HOD** (Head of Department) | Senior professor running a department | Approves teachers that belong to their department. Can view and edit academic structure. Builds timetables for their department's divisions. |
| **TIMETABLE_COORDINATOR** | Faculty volunteer assigned to scheduling | Same timetable-building powers as HOD, but cannot approve teachers or manage staff. |
| **TEACHER** | Regular faculty member | Sees their personal weekly timetable (read-only). Updates profile, changes password, sets weekly availability preferences. |

The fifth implicit role is **Student** — currently read-only through published PDFs and (in the future) a dedicated student page.

## 1.4 Key Features & Value Proposition

### Academic Structure Management
- Multi-year support — past years stay archived, current year is editable, future years can be pre-built
- Departments scoped to an academic year (so 2024-25 COMP can differ from 2025-26 COMP)
- Divisions belong to both a department and an academic year, carry student strength, year, branch name, class teacher, class representative, and choose a time-slot template (`TYPE_1` / `TYPE_2`)
- Batches (A1, A2, A3) split a division for labs

### Staff Lifecycle
- Bulk CSV staff upload with quote-aware parsing
- Manual staff creation with default password (`mitaoe@123` in dev, UUID in prod)
- Teacher self-registration using the MITAOE email domain
- Admin/HOD approval workflow — new teacher is inactive until approved
- First-login password change forced via `is_first_login` flag
- Teacher min/max weekly hours stored per teacher, checked by conflict service

### Timetable Builder
- Grid view: days as columns (Mon–Sat), time slots as rows
- Two time-slot profiles (`TYPE_1` and `TYPE_2`) let different divisions use different daily rhythms
- Break rows render as greyed-out merged cells that reject class bookings
- Click-to-add on empty cells, click-to-edit on filled cells
- Drag-and-drop to move an entry or swap with another
- Lab Session Wizard — single dialog creates 2×N entries (N batches × 2 consecutive periods) atomically
- Copy-from-division — clone a similar division's draft in one click
- Semester filter — see only SEM_3 entries, for example

### Conflict Detection (8 checks on every save)
1. **Break slot protection** — you cannot schedule a class in a slot marked as break
2. **Teacher conflict** — teacher already has another class that day/period
3. **Room conflict** — room already booked
4. **Division conflict** — division already has a class that day/period
5. **Teacher daily period limit** — configurable via `app.timetable.max-periods-per-day` (default 6)
6. **Teacher weekly hour limit** — uses `SUM(durationMinutes)` not just entry count, so 30-minute tutorials count correctly
7. **Room capacity** — rejects if division strength (or batch strength for labs) exceeds room capacity
8. **Room-course type match** — warns if THEORY course booked in LAB room, or LAB course booked in CLASSROOM

### Pre-Publish Validation Dashboard
- Empty-day warnings (e.g. "Saturday has no classes — intentional?")
- Empty core-hour warnings (morning periods with no class)
- Teacher weekly hours exceeded (blocking error)
- Teacher below minimum weekly hours (warning)
- Block publishing if any blocking errors exist; allow override for warnings only

### Exports
- **PDF (A4 landscape)** with MITAOE header, table with merged break rows, purple lab cells, grey break cells
- **Excel (.xlsx)** styled identically, auto-sized columns, merged cells
- Both available for division timetables and individual teacher timetables

### Security (production-hardened)
- JWT authentication, 1-hour token expiry
- BCrypt password hashing
- Account lockout after 5 failed attempts (15-minute lock)
- Input XSS sanitization via `@RestControllerAdvice`
- Tiered rate limiting (auth 10/min in prod, writes 30/min, reads 60/min)
- Password strength enforcement (8+ chars, upper, lower, digit, special)
- Security headers — HSTS, X-Frame-Options=DENY, X-Content-Type-Options=nosniff, Cache-Control
- Audit logs for admin actions with `[AUDIT]` prefix
- Environment-driven config — no hardcoded secrets in prod profile

---

# 2. Business Logic & Use Cases

This chapter walks through what *actually happens* inside SamaySetu for the real tasks a college schedule coordinator needs to perform.

## 2.1 Core Entities — The Mental Model

Before the workflows, know the nouns:

- **Academic Year** — "2025-26". Every division and department is scoped to one year.
- **Department** — "Computer Engineering". Belongs to one academic year (so departments can be re-created fresh each year).
- **Course** — "Data Structures (CS201)". Has a `courseType` of THEORY or LAB, a `semester` (SEM_1..SEM_8), and lives inside a department.
- **Division** — "COMP SY-A". Has a `year` (1–4), a `branch`, total student count, and chooses a `timeSlotType` (TYPE_1 or TYPE_2).
- **Batch** — "A1", "A2", "A3". A sub-group of a division used only for lab sessions.
- **Classroom / Room** — "H202". Has a type (CLASSROOM, LAB, AUDITORIUM), a capacity, and usually an owning department.
- **Time Slot** — "Period 3: 10:00–11:00". Has a `type` (TYPE_1 / TYPE_2) so different divisions can run different daily rhythms. May be `isBreak=true`.
- **Teacher / Faculty User** — a row in `users` with role TEACHER / HOD / TIMETABLE_COORDINATOR / ADMIN.
- **Teacher Availability** — optional preferences "I cannot teach Monday 3–5 pm", enforced during conflict detection.
- **Timetable Entry** — one cell in the grid. Joins: division + course + teacher + room + time slot + day of week + academic year. Has a `status` of DRAFT / PUBLISHED / ARCHIVED, an optional `semester`, an optional `batch`, and an optional `labSessionGroup`.
- **Lab Session Group** — a parent row that ties together the 2×N entries that constitute one lab session (N batches × 2 consecutive periods).

## 2.2 Workflow 1 — Standing Up a New Academic Year

1. **Admin logs in** → lands on `/admin/dashboard`.
2. **Creates the academic year** — `Academic Structure → Academic Years → + Add`. Fills name (`2025-26`), start date (01 Jul 2025), end date (30 Jun 2026), sets `isCurrent=true`. The UI auto-unsets `isCurrent` on previously current years so only one is current at a time.
3. **Creates departments** — `Academic Structure → Departments`. For each department (COMP, ENTC, MECH, CIVIL, AIDS), picks a name, 3-letter code, HOD name, and links to the new academic year.
4. **Creates courses** per department — can upload `.csv` via `Staff/Courses → Upload Courses` or add one by one. CSV columns: `Name, Code, Type, Credits, Hours Per Week, Semester`.
5. **Creates rooms** — rooms have a `buildingWing` auto-synced with the `roomNumber` prefix (room `H202` auto-picks wing `H`). Rooms belong to a department.
6. **Creates time slots** — typical MITAOE profile has 7 lecture periods and 3 breaks, split into TYPE_1 (e.g. first-half morning profile) and TYPE_2 (e.g. afternoon shift).
7. **Creates divisions** — one per year × branch × section. Picks its `timeSlotType`. Enters the `totalStudents` count.
8. **Creates batches** (if labs are run) — typically 3 batches per division.
9. **Uploads staff** — CSV upload or manual add. Sets min/max weekly hours, assigns to department.
10. **Ready to build** — go to **Timetable** and start adding entries.

## 2.3 Workflow 2 — Building a Division's Timetable

An admin or timetable coordinator opens `/admin/timetable`:

1. **Select academic year** — auto-defaults to the `isCurrent=true` year.
2. **Select division** — dropdown lists divisions scoped to the selected year.
3. **Optionally select semester** — narrows displayed entries.
4. The **draft entries** for that division load in a grid: rows = time slots for the division's `timeSlotType`, columns = Monday..Saturday.
5. **Click an empty cell** → Add Entry modal opens with day + slot prefilled. Admin picks course, teacher, room, optional batch. Submit sends `POST /api/timetable/entries`.
6. **Before saving**, the backend's `ConflictCheckService` runs 8 checks. If any conflict, the API returns HTTP 409 with a list of human-readable conflict messages. The admin fixes and retries.
7. **Click an existing entry** → same modal in edit mode. The entry can also be **dragged** to another empty cell, or **dragged onto another entry** to swap positions (two PUTs are fired back-to-back).
8. **Labs need the Lab Session Wizard**. Clicking "+ Lab Session" opens a wizard that:
   - Picks a lab course
   - Picks a starting period (system auto-reserves the next period as the second half)
   - For each batch in the division, picks a teacher + a lab room
   - Submits one `POST /api/timetable/lab-session` which atomically creates 2×N entries tied to a single `LabSessionGroup`
9. **Delete** — a delete button on each entry. Deleting a lab entry prompts "delete entire lab session group?" (all 2×N entries go together).
10. **Copy from another division** — one click copies every draft entry from a source division into the current division, skipping lab sessions.
11. **Refresh** — refetches the draft from backend (useful after another admin edits in parallel).

## 2.4 Workflow 3 — Publishing the Timetable

Draft entries are invisible to teachers and students. Only PUBLISHED entries are cached in Redis and served by `/api/timetable/division/{id}`.

1. **Click "Publish"** → frontend fires `GET /api/timetable/validate?divisionId=…`.
2. `TimetableValidationService` runs the pre-publish checks. Returns:
   - `errors[]` — blocking conditions (no entries at all, teacher weekly hours exceeded, etc.)
   - `warnings[]` — informational (empty Saturday, empty core slot, teacher below minimum hours)
   - `totalEntries`, `totalTeachers`, `daysScheduled` for summary
3. A **pre-publish modal** shows the result. If `errors.length > 0`, the "Publish Now" button is disabled.
4. On confirm, frontend calls `POST /api/timetable/publish?divisionId=…&academicYearId=…&force=false`. Service:
   - Archives any previously-PUBLISHED entries for that division + year (`ARCHIVED`)
   - Flips every DRAFT entry for the division + year to PUBLISHED
   - Evicts Redis caches (`timetable-division`, `timetable-teacher`)
5. Published data is now visible to teachers via `/api/timetable/teacher/{id}`.

## 2.5 Workflow 4 — Teacher Logs In

1. Teacher lands on `/login`, enters MITAOE email + password.
2. On success, `POST /auth/login` returns `{ email, token, role, firstLogin }`.
3. If `firstLogin=true`, frontend forces redirect to `/change-first-password`.
4. Otherwise, `TEACHER` → `/dashboard`, admin-layout roles → `/admin/dashboard`.
5. Dashboard shows **Today's classes**, **Weekly load**, and quick links.
6. `/dashboard/timetable` — GET `/api/timetable/teacher/{myId}?academicYearId=…` and render a grid.
7. `/dashboard/availability` — GET `/api/staff/availability` to load, PUT to save. Replaces all entries in a single transaction.
8. `/dashboard/profile` — view profile, edit phone / specialization (name and employee ID are read-only).
9. **Password change** — old + new + confirm. Current password verified with BCrypt; new password must pass strength rules and be different from old.

## 2.6 Workflow 5 — Teacher Self-Registration + Approval

1. New teacher goes to `/register`, enters name, employee ID, email (must end with `@mitaoe.ac.in`), phone, specialization, password.
2. Backend creates a `TeacherEntity` with `isEmailVerified=false`, `isApproved=false`, `isActive=false`, `role=TEACHER`. Generates a one-time verification token and emails the user.
3. User clicks the email link → `GET /auth/verify-email?token=…` → `isEmailVerified=true`, still pending approval.
4. Admin/HOD opens **Staff Management** → **Pending Approvals** → sees the new teacher.
5. Admin clicks **Approve** → `POST /api/teachers/{id}/approve` → `isApproved=true`, `isActive=true`, welcome email sent.
6. Teacher can now log in.

Edge case: if `isApproved=false` but the teacher tries to log in, `TeacherService.loadUserByUsername` throws a message: *"Your account is pending admin approval. Please wait for approval."* The frontend shows the reason as a toast.

## 2.7 Workflow 6 — Password Recovery

1. User goes to `/forgot-password`, enters email.
2. Backend generates a UUID reset token, stores it with a 1-hour expiry, and emails a link to the frontend URL.
3. User clicks the link → `/reset-password?token=…` page.
4. Frontend GETs `/auth/reset-password?token=…` to validate the token is still alive; backend redirects to frontend with error query param if expired.
5. User types new password + confirmation → `POST /auth/reset-password` → password updated, token cleared.

## 2.8 Edge Cases & Decision Logic

### Case: Lab sessions with batch splitting
- A lab session occupies **two consecutive periods** of a division.
- The division splits into N batches (typically 3). Each batch runs in parallel, each with its own teacher and its own lab room.
- That means one "lab session" = N×2 timetable entries (N batches × 2 periods).
- All those entries share the same `lab_session_group_id`. The conflict checker, when seeing this, skips the usual "teacher/division already booked" check because these are intentional parallel entries.

### Case: Break-slot protection
- Break time slots have `isBreak=true` in `time_slots`. They render in the grid as greyed banners spanning all six day columns.
- If you try to save an entry into a break slot, conflict check 0 fires first and rejects with `"Break slot: Cannot schedule a class during 'Lunch Break' — this is a break period."`

### Case: Teacher weekly hour limit using duration, not count
- Early versions counted entries (one entry = one hour). But some slots are 30 minutes (tutorials), and some are 120 minutes (double-period labs).
- Current logic: `SUM(timeSlot.durationMinutes)` across entries, then convert to hours. Projection includes the new slot's duration to reject *before* the save rather than after.

### Case: Divisions with different time-slot profiles
- First-shift divisions use `TYPE_1` (7:30–14:00). Second-shift divisions use `TYPE_2` (14:00–20:00).
- When rendering a division's grid, the frontend filters `timeSlots` by `slot.type === division.timeSlotType` (falling back to TYPE_1 if unset).
- Export services do the same filtering so a TYPE_2 division's PDF doesn't show TYPE_1 rows.

### Case: First login
- Admin-created users are born with `isFirstLogin=true`.
- On successful login, `AuthController` returns `firstLogin=true` only for TEACHER-role users (admins skip this).
- Frontend redirects to `/change-first-password`. On save, `isFirstLogin=false` and user can proceed.

### Case: Teacher availability conflict
- `TeacherAvailability` rows can mark any (day, start, end) as available or not. If a teacher has *any* availability rows for a day but none overlapping the requested slot, the conflict checker reports *"Teacher availability: This teacher is marked as unavailable on MONDAY at this time slot."*
- If the teacher has never entered availability, no rows exist for that day and the check is silently skipped (opt-in model).

### Case: Archiving before publishing again
- You publish COMP SY-A for 2025-26. Two weeks later, the schedule changes. You edit drafts and hit Publish again.
- The service doesn't delete the old PUBLISHED entries — it flips them to ARCHIVED, so history is preserved. Students who downloaded a PDF last week still see the timetable from their version; new loads see the new one.

### Case: Account lockout
- Five failed login attempts in a short window → `accountLockedUntil` is set 15 minutes into the future.
- Login endpoint checks `Timestamp.now() < accountLockedUntil` and returns *"Account locked. Try again after HH:MM"* without even running BCrypt.

---

# 3. System Architecture

## 3.1 High-Level Architecture

```
┌───────────────────────────────────────────────────────────┐
│                         BROWSER                           │
│  ┌─────────────────────────────────────────────────────┐  │
│  │     React SPA (Vite build, served by Amplify)       │  │
│  │  - Zustand store with persisted auth state          │  │
│  │  - Axios instance with JWT interceptor              │  │
│  │  - React Router (protected routes by role)          │  │
│  └─────────────────────────────────────────────────────┘  │
└──────────────────────┬────────────────────────────────────┘
                       │  HTTPS (CORS allowed origins)
                       │  Authorization: Bearer <jwt>
                       ▼
┌───────────────────────────────────────────────────────────┐
│                SPRING BOOT APPLICATION                    │
│  ┌──────────────────────────────────────────────────────┐ │
│  │  Filters:                                            │ │
│  │    RateLimitFilter  →  JWTRequestFilter  →  Routing  │ │
│  └──────────────────────────────────────────────────────┘ │
│  ┌──────────────────────────────────────────────────────┐ │
│  │  Controllers (REST endpoints)                        │ │
│  │    /auth, /admin, /admin/api/*, /api/*               │ │
│  └──────────────────────────────────────────────────────┘ │
│  ┌──────────────────────────────────────────────────────┐ │
│  │  Services (business logic)                           │ │
│  │    TimetableService, ConflictCheckService,           │ │
│  │    ValidationService, ExportService, AuditLogService │ │
│  └──────────────────────────────────────────────────────┘ │
│  ┌──────────────────────────────────────────────────────┐ │
│  │  Repositories (Spring Data JPA)                      │ │
│  └──────────────────────────────────────────────────────┘ │
└──────┬──────────────────────────────────┬─────────────────┘
       │ JDBC                             │ Lettuce (Redis client)
       ▼                                  ▼
┌──────────────────────┐      ┌──────────────────────────┐
│    PostgreSQL 17     │      │         Redis            │
│  - Academic data     │      │  - Cached timetables     │
│  - Users             │      │  - Rate-limit buckets    │
│  - Timetable entries │      │  - TTL 10 min            │
└──────────────────────┘      └──────────────────────────┘
       │
       ▼
┌──────────────────────┐
│  Gmail SMTP          │
│  (verify / approve / │
│   forgot-password)   │
└──────────────────────┘
```

### Request lifecycle — a single GET

Consider `GET /admin/api/departments` with an admin JWT.

1. **CORS preflight** (`OPTIONS`) is answered by the `CorsFilter` bean, checking against `app.cors.allowed-origins`.
2. **RateLimitFilter** — passes straight through because this path doesn't match `/auth/**`.
3. **JWTRequestFilter** — extracts the Bearer token, calls `JWTUtil.extractUsername`, loads the user via `TeacherService.loadUserByUsername`, runs `jwtUtil.validateToken` against the user, and sets `SecurityContextHolder` with `UsernamePasswordAuthenticationToken` carrying the authority `ROLE_ADMIN`.
4. **Spring Security authorization** — `SecurityConfig.authorizeHttpRequests` sees `/admin/**` and requires `hasRole("ADMIN")`. Match → pass.
5. **Controller method** — `DepartmentController.getAllDepartments()` invokes `departmentService.getAll()`.
6. **Repository** — Spring Data JPA's `findAll()` issues a SELECT.
7. **Serialisation** — Jackson converts the list of `DepartmentEntity` to JSON, using the `@JsonIgnoreProperties` annotations to strip back-references so the response stays a tree (no cycles).
8. **Response** — `200 OK` with JSON body, `Content-Type: application/json`.

### Request lifecycle — a POST that writes

`POST /api/timetable/entries` with a body.

1. Filters 1–3 as above.
2. Authorization — the method has `@PreAuthorize("hasRole('ADMIN')")` (or similar) in addition to the URL matcher.
3. **Bean validation** — `@Valid @RequestBody CreateTimetableEntryDTO` triggers JSR-380 validation; missing required fields get rejected with 400 before hitting the service.
4. **InputSanitizationAdvice** — strips HTML-unsafe characters from every incoming String property on request DTOs (XSS defence-in-depth).
5. **Controller** — calls `timetableService.addEntry(dto)`.
6. **Service** — loads the related entities (division, course, teacher, room, time slot, academic year), builds a `TimetableEntryRequest`, invokes `ConflictCheckService.checkConflicts`. If any conflict, it throws `TimetableConflictException`.
7. **Persistence** — if clean, saves the entry (`status=DRAFT` by default).
8. **Cache eviction** — because drafts can eventually be published, the write path does NOT pre-emptively evict publish caches (the publish flow handles that).
9. **Response** — `201 Created` with the new entity, or `409 Conflict` with `{ "message": "...", "conflicts": [ "Teacher conflict: …", … ] }`.
10. **Global exception handler** — `GlobalExceptionHandler` turns any uncaught exception into a safe JSON error so sensitive stack traces never leak.

## 3.2 Tech Stack Summary

| Layer | Choice | Why |
|-------|--------|-----|
| Backend language | Java 17 | LTS, Lombok-compatible, required by Spring Boot 3.x |
| Backend framework | Spring Boot 3.5.5 | Mature ecosystem, starters for every need |
| Persistence | Spring Data JPA + Hibernate | Declarative repositories, validation, auditing hooks |
| Database | PostgreSQL 17 | Strong relational guarantees, enum support, JSONB for future extensions |
| Cache | Redis (Memurai on Windows) | Low-latency reads for published timetables |
| Security | Spring Security + JJWT 0.12.6 | Industry-standard JWT implementation |
| Password hashing | BCryptPasswordEncoder | Slow hash, per-password salt |
| PDF | OpenPDF 1.3.35 | LGPL, no iText licensing issue |
| Excel | Apache POI 5.2.5 | Native XSSF workbook API |
| Build | Maven 3.6+ | Declarative, wraps itself (`mvnw`) |
| Frontend language | TypeScript 5.2 | Type safety; strict mode on |
| Frontend framework | React 18.3.1 | Hooks, concurrent rendering |
| Frontend bundler | Vite 5.1 | Fast dev server, ESBuild transforms |
| Routing | react-router-dom 6.22 | Nested routes, `Navigate` redirect |
| State | Zustand 4.5 + `persist` middleware | Simpler than Redux, built-in local/session storage persistence |
| HTTP client | Axios 1.6 | Interceptor support for JWT injection |
| Drag-and-drop | `@dnd-kit/core` 6.3 | Accessible, keyboard-aware, supports touch |
| Animation | Framer Motion 11 | Sidebar transitions |
| Toasts | react-hot-toast 2.4 | Global toast queue |
| Styling | Tailwind CSS 3.4 + custom primary-800 brand color | Utility-first, no CSS-in-JS overhead |
| CI | GitHub Actions | Free, repo-native |
| Deployment (backend) | AWS EC2 Auto Scaling Group + ALB | Elastic, zero-downtime rolling updates |
| Deployment (frontend) | AWS Amplify | Auto-build from Git, CDN, env vars |

## 3.3 Key Architectural Decisions (and Why)

### 3.3.1 Stateless JWT instead of server sessions
- **Why**: The backend runs behind an auto-scaling group; session stickiness and session replication add operational pain.
- **Trade-off**: Cannot revoke tokens server-side. Mitigated with 1-hour expiry and account lockout.

### 3.3.2 `ddl-auto=update` in dev, `validate` in prod
- **Why**: Fast iteration in dev (entity edits reflect immediately); safety in prod (no accidental destructive ALTERs).
- **Trade-off**: Production DDL must be handled by a migration tool. Flyway is already wired in `pom.xml` (disabled by default); the path to enable it is documented in `application-dev.properties`.

### 3.3.3 Redis caching for reads only
- **Why**: Published timetables are read hundreds of times per day but changed only when an admin re-publishes. Perfect fit for read-through cache.
- **Eviction**: `@CacheEvict` decorates the publish, archive, and update methods.
- **TTL fallback**: 10 minutes, so even if eviction misses, staleness is bounded.

### 3.3.4 `open-in-view=false`
- **Why**: Keep DB sessions tight. Without this, a lazy-loaded association deep in the view layer would re-open a transaction and mask N+1 issues.
- **Trade-off**: Every association accessed during serialization must be `EAGER` or explicitly fetched in the query. The codebase chooses `EAGER` on the associations that are serialized (room, course, teacher, etc.) and uses `@JsonIgnore` on the back-refs.

### 3.3.5 Two time-slot templates (TYPE_1, TYPE_2)
- **Why**: MITAOE runs a first-half and a second-half shift with different daily rhythms. Instead of conditional logic, the schema carries a `type` on every slot and a `timeSlotType` on every division. The division picks which template it follows.

### 3.3.6 Lab session group as a first-class entity
- **Why**: A lab session is *atomic* at the business level (all batches + both periods), but stored as N×2 separate rows for query simplicity (the teacher's grid only needs to query `timetable_entries`, not a compound structure).
- **Trade-off**: Deletion has to cascade across the group.

### 3.3.7 Role-based security with annotation + URL matchers
- **Why**: URL matchers in `SecurityConfig` catch broad paths (`/admin/**`); `@PreAuthorize` on method signatures adds tighter, declarative guards that also work with method-level refactors.

### 3.3.8 Frontend Zustand over Redux Toolkit
- **Why**: SamaySetu's global state is tiny (auth user + hydration flag). Redux boilerplate is overkill. Zustand with `persist` gives free localStorage/sessionStorage sync and a hydration flag to prevent flicker.

### 3.3.9 Password default in dev vs prod
- **Why**: Developers need a known password to test login without fetching emails. Production cannot ship with `mitaoe@123`. The property `app.staff.default-password` picks either a literal (dev) or the keyword `random` (prod → UUID).

### 3.3.10 Dedicated "Staff" role model with `TeacherEntity` behind `users` table
- **Why**: All four non-admin roles share the same user shape (email, password, department, hours). Putting them in one table with a `role` column keeps JOINs trivial. The entity is named `TeacherEntity` for historical reasons but the table is `users`.

---

# 4. Folder Structure & Codebase Walkthrough

## 4.1 Top-Level Layout

```
SamaySetu_Connect_plan_manage/
├── .github/                      # GitHub Actions workflows (CI, deploy)
├── AWS deployment files/         # CloudFormation / user-data scripts
├── Backend/                      # Spring Boot project
├── Files/                        # Design mockups, planning docs (not shipped)
├── Frontend/                     # React + TS SPA
├── mysql_backups/                # Legacy MySQL dumps from early prototype
├── Scripts/                      # SQL seed scripts for dev
├── tf-env/                       # Terraform environment configs (infrastructure)
├── .gitignore
├── README.md                     # Quick-start README
├── SamaySetu_Postman_Collection.json   # Postman API collection
├── SamaySetu_Postman_Environment.json
├── amplify.yml                   # AWS Amplify build spec for the frontend
├── database.txt                  # Historical DB notes
└── SAMAYSETU_PROJECT_BIBLE.md    # ← This document
```

## 4.2 Backend Layout

```
Backend/
├── mvnw, mvnw.cmd                # Maven wrapper — build without installing Maven
├── pom.xml                       # Maven project descriptor (deps, Java version)
├── src/main/java/com/College/timetable/
│   ├── samaysetuApplication.java       # Spring Boot entry point, @EnableCaching
│   │
│   ├── Configuration/
│   │   ├── SecurityConfig.java         # Spring Security filter chain, CORS, access rules
│   │   ├── RedisConfig.java            # Redis cache manager, serializer config
│   │   └── InputSanitizationAdvice.java# Global XSS sanitisation on request bodies
│   │
│   ├── Controller/                     # @RestController classes — every HTTP endpoint
│   │   ├── AuthController.java               POST /auth/login, /register, /reset, etc.
│   │   ├── AcademicController.java           /admin/api/academic-years + public read
│   │   ├── DepartmentController.java         /admin/api/departments
│   │   ├── CourseController.java             /admin/api/courses
│   │   ├── DivisionController.java           /admin/api/divisions
│   │   ├── RoomController.java               /admin/api/rooms
│   │   ├── TimeSlotController.java           /admin/api/time-slots + public read
│   │   ├── BatchController.java              /admin/api/batches
│   │   ├── TeacherController.java            /api/teachers + approve/reject
│   │   ├── StaffController.java              /api/staff/* profile & availability
│   │   ├── AdminController.java              /admin/* CSV upload, manual create
│   │   └── TimetableController.java          /api/timetable/* all timetable ops
│   │
│   ├── Entity/                         # @Entity JPA classes — one table each
│   │   ├── AcademicYear.java
│   │   ├── DepartmentEntity.java
│   │   ├── CourseEntity.java
│   │   ├── Division.java
│   │   ├── Batch.java
│   │   ├── ClassRoom.java
│   │   ├── TimeSlot.java
│   │   ├── Student.java
│   │   ├── TeacherEntity.java                Maps to `users` table, implements UserDetails
│   │   ├── TeacherCred.java                  Legacy credential helper (minor)
│   │   ├── TeacherAvailability.java          One row per weekly availability slice
│   │   ├── TimetableEntry.java               Core timetable row
│   │   ├── LabSessionGroup.java              Parent of N×2 lab entries
│   │   ├── CourseType.java  (enum)           THEORY / LAB
│   │   ├── RoomType.java    (enum)           CLASSROOM / LAB / AUDITORIUM
│   │   ├── DayOfWeek.java   (enum)           MONDAY..SATURDAY
│   │   ├── Semester.java    (enum)           SEM_1..SEM_8
│   │   └── TimetableStatus.java (enum)       DRAFT / PUBLISHED / ARCHIVED
│   │
│   ├── Repository/                     # Spring Data JPA interfaces
│   │   ├── AcademicYearRepository.java
│   │   ├── Dep_repo.java
│   │   ├── Course_repo.java
│   │   ├── Division_repo.java
│   │   ├── Batch_repo.java
│   │   ├── Room_repo.java
│   │   ├── TimeSlot_repo.java
│   │   ├── Student_repo.java
│   │   ├── Teacher_Repo.java
│   │   ├── TeacherAvailability_repo.java
│   │   ├── TimetableEntry_repo.java          Custom @Query methods for conflict checks
│   │   └── Lab_session_repo.java
│   │
│   ├── Service/                        # Business-logic layer (@Service)
│   │   ├── TeacherService.java               UserDetailsService + CRUD for teachers
│   │   ├── AdminService.java                 CSV parsing, bulk teacher create
│   │   ├── AcademicService.java              Academic year CRUD + isCurrent invariant
│   │   ├── DepartmentService.java
│   │   ├── CourseService.java
│   │   ├── DivisionService.java
│   │   ├── RoomService.java
│   │   ├── TimeSlotService.java
│   │   ├── TimetableService.java             Create/update/delete entries, publish/archive
│   │   ├── ConflictCheckService.java         The 8-point conflict detection
│   │   ├── TimetableValidationService.java   Pre-publish dashboard
│   │   ├── TimetableExportService.java       PDF + Excel generation
│   │   ├── TableService.java                 Admin convenience helpers
│   │   ├── EmailService.java                 Verification, welcome, approval, rejection emails
│   │   └── AuditLogService.java              Structured [AUDIT] logs for admin actions
│   │
│   ├── Filter/
│   │   ├── JWTRequestFilter.java             Parses Bearer, sets SecurityContext
│   │   └── RateLimitFilter.java              Per-IP sliding window for /auth/**
│   │
│   ├── Exception/
│   │   └── GlobalExceptionHandler.java       @RestControllerAdvice — maps exceptions to JSON
│   │
│   ├── IO/                             # DTOs (Data Transfer Objects)
│   │   ├── AuthRequest.java                  Login body
│   │   ├── AuthResponse.java                 { email, token, role, firstLogin }
│   │   ├── RegisterRequest.java              Self-registration body
│   │   ├── ForgotPasswordRequest.java
│   │   ├── ResetPasswordRequest.java
│   │   ├── ChangePasswordRequest.java        Authenticated password change
│   │   ├── ChangeFirstPasswordRequest.java   First-login password change
│   │   ├── ProfileUpdateRequest.java         Admin → full update
│   │   ├── StaffProfileUpdateRequest.java    Teacher → limited update (phone, specialization)
│   │   ├── AdminStaffUpdateRequest.java
│   │   ├── ManualStaffRequest.java           Admin "+ New Staff" form
│   │   ├── AvailabilityDTO.java
│   │   ├── CreateTimetableEntryDTO.java
│   │   ├── CreateLabSessionGroupDTO.java
│   │   ├── CreateLabSessionRequest.java
│   │   └── TimetableEntryRequest.java
│   │
│   └── Util/
│       ├── JWTUtil.java                      Token issue + validate
│       ├── DataInitializer.java              Seeds default admin on first boot
│       └── TimetableConflictException.java   Carries list of conflicts across the stack
│
└── src/main/resources/
    ├── application.properties                 Only picks the active profile
    ├── application-dev.properties             Dev DB, logs DEBUG, known staff password
    └── application-prod.properties            All secrets from env vars, ddl-auto=validate
```

## 4.3 Frontend Layout

```
Frontend/
├── package.json                # Dependencies + build scripts
├── vite.config.ts              # Vite plugin config
├── tsconfig.json / tsconfig.app.json / tsconfig.node.json
├── tailwind.config.js          # primary-800 brand color, custom screens
├── postcss.config.js
├── index.html                  # Single mount point (#root)
├── public/                     # Static assets (logo, favicon)
├── md files/                   # Team-internal planning markdowns
└── src/
    ├── main.tsx                # Vite entry — ReactDOM.createRoot
    ├── App.tsx                 # Top-level router, ErrorBoundary, hydration gate
    ├── App.css, index.css      # Global styles, Tailwind directives
    ├── vite-env.d.ts
    ├── assets/                 # Images (logo, banner video)
    ├── constants/
    │   └── index.ts            # COLLEGE_EMAIL_DOMAIN, role names, URLs
    ├── hooks/                  # Reusable hooks (form, toast, etc.)
    ├── services/
    │   └── api.ts              # Axios instance + JWT interceptor + all API clients
    ├── store/
    │   └── authStore.ts        # Zustand — user + rememberMe + dynamic storage
    ├── types/
    │   └── index.ts            # Shared TS interfaces (User, Division, etc.)
    ├── utils/
    │   └── errorHandler.ts     # getErrorMessage() — extracts readable text from axios error
    ├── pages/                  # Top-level routed pages
    │   ├── LoginPage.tsx
    │   ├── RegisterPage.tsx
    │   ├── VerifyEmailPage.tsx
    │   ├── ForgotPasswordPage.tsx
    │   ├── ResetPasswordPage.tsx
    │   ├── ChangeFirstPasswordPage.tsx
    │   ├── TeacherDashboard.tsx   Shell (Navbar+Sidebar) with child <Routes>
    │   ├── AdminDashboard.tsx     Same, for admin-layout roles
    │   ├── DashboardPage.tsx      Dashboard home (teacher) — today's classes, weekly load
    │   ├── admin/
    │   │   └── AdminProfilePage.tsx
    │   └── teacher/
    │       ├── TimetablePage.tsx      Teacher's personal weekly grid
    │       ├── AvailabilityPage.tsx   Weekly availability editor
    │       └── ProfilePage.tsx        Own profile + change password
    └── components/
        ├── admin/
        │   ├── AdminDashboardHome.tsx
        │   ├── AcademicStructurePage.tsx    Tabs for all academic entities
        │   ├── AcademicYearsPage.tsx
        │   ├── DepartmentsPage.tsx
        │   ├── CoursesPage.tsx
        │   ├── DivisionsPage.tsx
        │   ├── RoomsPage.tsx
        │   ├── TimeSlotsPage.tsx
        │   ├── StaffManagementPage.tsx       CSV upload + manual + approvals
        │   ├── TeachersPage.tsx              Legacy — redirects to Staff
        │   └── TimetableManagementPage.tsx   The big grid builder
        ├── auth/
        │   └── ProtectedRoute.tsx            Role-aware route guard
        ├── common/
        │   ├── Button.tsx, Card.tsx, Input.tsx, Modal.tsx, Loading.tsx, ErrorBoundary.tsx
        ├── dashboard/
        │   └── StatsCard.tsx                 Metric tile used on dashboard home
        └── layout/
            ├── Navbar.tsx                     Top bar with profile dropdown
            └── Sidebar.tsx                    Left navigation, role-filtered links
```

## 4.4 How the parts interact

- **`main.tsx`** wraps `App.tsx` in a BrowserRouter and renders to `#root`.
- **`App.tsx`** reads `useAuthStore` to check auth state and waits for `_hasHydrated` to avoid a flash of "you're logged out". Then:
  - `/login` | `/register` | `/verify-email` | `/forgot-password` | `/reset-password` | `/change-first-password` → public components.
  - `/dashboard/*` → `<ProtectedRoute allowedRoles={['TEACHER']}>` wraps `<TeacherDashboard>`.
  - `/admin/*` → `<ProtectedRoute allowedRoles={['ADMIN', 'HOD', 'TIMETABLE_COORDINATOR']}>` wraps `<AdminDashboard>`.
- **`TeacherDashboard.tsx`** / **`AdminDashboard.tsx`** each render `<Navbar>`, `<Sidebar>`, and a `<main>` with their own nested `<Routes>` for sub-pages (profile, timetable, etc.).
- Every API call flows through **`services/api.ts`**, which:
  1. Attaches the JWT from localStorage *or* sessionStorage (whichever has it) in a request interceptor.
  2. On 401, wipes both storages and redirects to `/login` (guarded by `isRedirectingToLogin` so multiple concurrent 401s don't fire multiple redirects).
  3. On structured 403 `{ "error": "AccessDenied" }`, same treatment.

---

# 5. Module-by-Module Breakdown

## 5.1 Backend modules

### 5.1.1 `samaysetuApplication`
- **Purpose**: Spring Boot bootstrap class.
- **Key annotations**: `@SpringBootApplication`, `@EnableCaching` (turns on `@Cacheable`/`@CacheEvict`).
- **Output**: Runs the embedded Tomcat on `server.port` (default 8083).

### 5.1.2 `Configuration/SecurityConfig`
- **Purpose**: Declares the filter chain, CORS, auth rules, exception handlers.
- **Critical rules**:
  - `/auth/**`, `/api/timetable/manual`, `/actuator/health` → permitAll
  - `/api/teachers/**` → `hasAnyRole("TEACHER","ADMIN")`
  - `/admin/**` → `hasRole("ADMIN")`
  - anything else → authenticated
- **Filter order**: `RateLimitFilter` → `JWTRequestFilter` → `UsernamePasswordAuthenticationFilter` (default location).
- **Access denied handler**: returns a structured JSON body so the frontend can distinguish "auth lost → redirect to login" from "you're missing a role → show toast".
- **Bean**: `AuthenticationManager` backed by `DaoAuthenticationProvider` with `TeacherService` as UserDetailsService and `BCryptPasswordEncoder`.

### 5.1.3 `Configuration/RedisConfig`
- **Purpose**: Sets up the `RedisCacheManager` with a custom serializer that handles `java.sql.Timestamp` safely.
- **Caveat**: Without this config, Redis would reject timestamps because of Spring's default polymorphic validator.

### 5.1.4 `Configuration/InputSanitizationAdvice`
- **Purpose**: `@RestControllerAdvice` that runs `Jsoup.clean` (or similar) across String fields in request DTOs to neutralise XSS payloads.
- **Applies to**: Every controller method that accepts `@RequestBody`.

### 5.1.5 `Filter/JWTRequestFilter`
- **Inputs**: HTTP request with possibly-present `Authorization: Bearer ...` header.
- **Logic**:
  1. If path starts with `/auth`, skip.
  2. Extract bearer token.
  3. `jwtUtil.extractUsername(token)` → email.
  4. `teacherService.loadUserByUsername(email)` → `UserDetails`.
  5. `jwtUtil.validateToken(token, userDetails)` → boolean.
  6. If valid, set `SecurityContextHolder` with authorities.
- **Outputs**: Filter chain continues. On failure, it logs a warning and continues without auth (so `anyRequest().authenticated()` cleanly rejects).

### 5.1.6 `Filter/RateLimitFilter`
- **Inputs**: HTTP request.
- **Logic**: Only applies to `/auth/**`. Keeps an in-memory `ConcurrentHashMap<ipAddress, RequestTracker>`. Each tracker holds `windowStart` and an `AtomicInteger` count. Configurable via `app.rate-limit.max-requests` and `app.rate-limit.window-ms`.
- **Outputs**: HTTP 429 with JSON body when exceeded.

### 5.1.7 `Controller/AuthController`
- `POST /auth/login` — validates credentials, returns JWT + role + `firstLogin` flag. Emits `[LOGIN]` structured logs for every step so failures are traceable.
- `POST /auth/register` — self-registration (MITAOE email only). Creates inactive teacher + verification token + email.
- `GET /auth/verify-email?token=...` — sets `isEmailVerified=true`.
- `POST /auth/forgot-password` — generates reset token, emails link.
- `POST /auth/reset-password` — validates token, updates password.
- `GET /auth/reset-password?token=...` — checks token liveness and redirects to frontend with `/reset-password?token=…` or `?error=…`.
- `POST /auth/change-first-password` — for users with `isFirstLogin=true`.

### 5.1.8 `Controller/AdminController`
- All methods guarded by `@PreAuthorize("hasRole('ADMIN')")`.
- `POST /admin/upload-staff` — multipart CSV, validates size (≤5MB) and type, invokes `adminService.createStaffFromCSV`.
- `POST /admin/create-staff` — single-user manual create. Assigns role, resolves department, stamps `isApproved=true, isEmailVerified=true, isFirstLogin=true`.
- `GET /admin/download-staff-template` — serves a fixed-template CSV string.
- `PUT /admin/update-staff/{id}` — admin-wide edit of a staff row.
- `POST /admin/upload-courses` — bulk course CSV per department + year.
- `GET /admin/download-courses-template` — template CSV.

### 5.1.9 `Controller/TimetableController`
Broadest controller — all timetable operations live here.
- Read: `GET /api/timetable/division/{id}`, `.../teacher/{id}`.
- Draft read (admin-only): `GET /api/timetable/draft?divisionId=...`.
- Write (admin-only, conflict-checked):
  - `POST /api/timetable/entries` — adds one entry.
  - `PUT /api/timetable/entries/{id}` — updates (also used for drag-and-drop move + swap).
  - `DELETE /api/timetable/entries/{id}`.
  - `DELETE /api/timetable/draft?...` — bulk delete all DRAFT entries for a division + year.
  - `POST /api/timetable/lab-session` — atomic wizard endpoint.
  - `POST /api/timetable/lab-groups` — lower-level lab group creation.
  - `DELETE /api/timetable/lab-groups/{groupId}` — cascades to all entries.
  - `POST /api/timetable/copy?sourceDivisionId=...&targetDivisionId=...&...`.
- Validation & publish:
  - `GET /api/timetable/validate?divisionId=...` — returns `ValidationResult`.
  - `POST /api/timetable/publish?divisionId=...&...&force=false` — publishes (or rejects if errors exist and `force=false`).
  - `POST /api/timetable/archive?divisionId=...&...` — archive manually.
- Export (admin-role):
  - `GET /api/timetable/export/division/{id}/pdf` / `/excel`
  - `GET /api/timetable/export/teacher/{id}/pdf` / `/excel`

### 5.1.10 `Service/TeacherService`
- Implements `UserDetailsService`. `loadUserByUsername(email)` loads the user by email, performs null-safe boolean checks (email verified, approved, active), builds a Spring Security `User` with a single authority `ROLE_<role.toUpperCase()>`.
- `register`, `verifyEmail`, `forgotPassword`, `resetPassword`, `validateResetToken`, `changePassword`, `updateFirstLoginPassword`.
- Admin CRUD: `add`, `update`, `delete`, `approveTeacher`, `rejectTeacher`, `getPendingApprovals`.

### 5.1.11 `Service/ConflictCheckService`
Already shown in Chapter 2. Eight-point check returns the full list of conflicts so the admin fixes everything at once.

### 5.1.12 `Service/TimetableValidationService`
Runs a broader set of checks before publish. Not about individual entries — about the *whole division's draft*:
- "No entries" → blocks
- "Empty day" → warns
- "Empty core slot during mornings" → warns
- "Teacher over max weekly hours" → blocks
- "Teacher below min weekly hours" → warns
- "Lab session not using a LAB-type room" → warns

### 5.1.13 `Service/TimetableService`
Central CRUD + status lifecycle:
- `addEntry(dto)` → build `TimetableEntryRequest`, run conflict check, save with `status=DRAFT`.
- `updateEntry(id, dto)` → same, but exclude the current entry from conflict checks.
- `deleteEntry(id)` → basic delete.
- `publishDivisionTimetable(divisionId, academicYearId, force)` → flip status to PUBLISHED, archive previous, evict caches.
- `archivePublishedTimetable(divisionId, academicYearId)` → flip PUBLISHED → ARCHIVED.
- `clearDraft(divisionId, academicYearId)`.
- `copyFromDivision(sourceId, targetId, academicYearId)` — copies every DRAFT entry from source to target, skipping lab sessions (which usually need different batch/teacher mapping).
- `getDraftTimetable`, `getDivisionTimetable` (cached), `getTeacherTimetable` (cached).
- `createLabSession(req)` — atomic N×2 creation in a single transaction.
- `deleteLabGroup(groupId)` — cascades.

### 5.1.14 `Service/TimetableExportService`
- `generateDivisionPDF(divisionId, academicYearId)` — loads PUBLISHED entries, the division's slots, builds the PDF with OpenPDF.
- `generateDivisionExcel(...)` — same via Apache POI XSSFWorkbook.
- `generateTeacherPDF/Excel(...)` — same shape but for a single teacher.
- Both run on PUBLISHED data only (so draft-in-progress isn't leaked).

### 5.1.15 `Service/AuditLogService`
Emits structured logs prefixed with `[AUDIT]` for admin-sensitive actions: login success/failure, staff create, approval, publish, archive.

### 5.1.16 `Service/EmailService`
Uses `JavaMailSender`. Sends:
- `sendVerificationEmail(email, token)`
- `sendWelcomeEmail(email, name)` — after email verified
- `sendApprovalEmail(email, name)` — after admin approval
- `sendRejectionEmail(email, name, reason)`
- `sendPasswordResetEmail(email, token)`

All emails are sent through Gmail SMTP (`smtp.gmail.com:587`) via app passwords. In dev, this can be skipped via an env var pointing to a local SMTP sink (e.g. MailHog).

### 5.1.17 `Util/JWTUtil`
- Loads the Base64-encoded `jwt.secret.key` from config, decodes into a `javax.crypto.SecretKey`.
- `generateToken(userDetails)` — subject = email, iat = now, exp = now + 1 hour.
- `extractUsername(token)` / `validateToken(token, userDetails)`.

### 5.1.18 `Util/DataInitializer`
- `@CommandLineRunner` that on first boot seeds a single admin account using `app.admin.email` / `app.admin.password` (defaults `suryankadmin@mitaoe.ac.in` / `Admin@123` in dev).
- Only inserts if the email doesn't already exist — idempotent.

### 5.1.19 `Exception/GlobalExceptionHandler`
`@RestControllerAdvice` that maps any unhandled exception to a JSON body:
```
{ "status": 500, "error": "InternalServerError", "message": "...", "timestamp": "..." }
```
Prevents stack traces from leaking. Specialises on `ResponseStatusException`, `TimetableConflictException`, `MethodArgumentNotValidException` (for `@Valid` failures).

## 5.2 Frontend modules

### 5.2.1 `services/api.ts`
- Creates a single Axios instance bound to `VITE_API_URL` (falls back to `http://localhost:8083`).
- **Request interceptor**: attaches `Authorization: Bearer <jwt>` from `getToken()` which checks localStorage **then** sessionStorage. Skips `/auth/**` URLs.
- **Response interceptor**:
  - On 401 → calls `forceLogout()` which clears storage + redirects to `/login`. A module-scope `isRedirectingToLogin` boolean ensures multiple concurrent 401s only trigger one redirect.
  - On 403 — inspects the response body. If it's the structured `{ "error": "AccessDenied" }` / `"AuthRequired"` shape from `SecurityConfig`, calls `forceLogout`. Otherwise leaves the 403 alone so validation failures aren't misinterpreted as auth failures.
- Exports typed API clients per domain: `authAPI`, `teacherAPI`, `teacherAdminAPI`, `departmentAPI`, `courseAPI`, `roomAPI`, `divisionAPI`, `timeSlotAPI`, `timeSlotPublicAPI`, `batchAPI`, `academicYearAPI`, `timetableAPI`, `staffAPI`, `adminAPI`.

### 5.2.2 `store/authStore.ts`
- Zustand store with `persist` middleware.
- **State**: `user: { email, role, token, name? } | null`, `isAuthenticated`, `rememberMe`, `_hasHydrated`.
- **Actions**: `login(user, rememberMe)`, `logout()`, `setRememberMe(remember)`.
- **Storage strategy**: dynamic — `rememberMe=true` stores in localStorage, `rememberMe=false` stores in sessionStorage. On rehydrate, rewrites `jwt_token` into the right storage so page reloads + fresh tabs both see the token.

### 5.2.3 `components/auth/ProtectedRoute.tsx`
- Wraps a child with a role guard.
- If `!isAuthenticated` → `<Navigate to="/login">`.
- If `user.role` isn't in `allowedRoles` → `<Navigate to={getHomePath(user.role)}>`.
- Exports `getHomePath(role)` — `ADMIN`/`HOD`/`TIMETABLE_COORDINATOR` → `/admin/dashboard`, `TEACHER` → `/dashboard`, else `/login`.

### 5.2.4 `components/layout/Navbar.tsx`
- Top bar with logo, profile dropdown (name, email, role, "Profile" and "Logout" links).
- Mobile hamburger toggles sidebar visibility.

### 5.2.5 `components/layout/Sidebar.tsx`
- Fixed on desktop, slide-in on mobile via Framer Motion.
- Role-aware link list — e.g. HOD sees Staff but not Time Slots; TIMETABLE_COORDINATOR sees Time Slots but not Staff.

### 5.2.6 `components/admin/TimetableManagementPage.tsx`
- The grid builder described in §2.3.
- Uses `@dnd-kit/core` for drag-and-drop — each entry card is `useDraggable`, each cell is `useDroppable`, lab-group entries are `disabled: true` because swapping them would need to move 2×N entries at once.
- A top bar of action buttons: Refresh, Copy From Division, Clear Draft, Archive Published, PDF, Excel, Lab Session, Publish.
- A selector row: Academic Year | Division | Semester.
- The grid itself is a simple `<table>` with `<DroppableCell>` wrappers.

### 5.2.7 `components/admin/AcademicStructurePage.tsx`
- A tabbed wrapper that hosts `AcademicYearsPage`, `DepartmentsPage`, `CoursesPage`, `DivisionsPage`.
- Each tab is its own small CRUD page with its own API calls. They share common components (Card, Button, Modal, Input) for consistent look-and-feel.

### 5.2.8 `components/admin/StaffManagementPage.tsx`
- Three tabs: list, CSV upload, manual add.
- Rows show name, email, role, department, active/verified badges.
- Edit button opens a modal with profile fields + role dropdown.
- Pending-approvals section (for ADMIN/HOD) shows self-registered teachers awaiting verification.

### 5.2.9 `pages/teacher/TimetablePage.tsx`
- Loads the teacher's published timetable via `/api/timetable/teacher/{myId}?...`.
- Renders the same grid component shape as admin, but read-only.

### 5.2.10 `pages/teacher/AvailabilityPage.tsx`
- 6-day × N-slot grid of toggleable checkboxes.
- Saves via `PUT /api/staff/availability` which atomically deletes-then-inserts all rows.

### 5.2.11 `pages/admin/AdminProfilePage.tsx`, `pages/teacher/ProfilePage.tsx`
- View basic profile.
- Edit limited fields (phone, specialization).
- Change password via `/api/staff/change-password`.

---

# 6. Database Design

## 6.1 Entity–Relationship Overview

```
AcademicYear 1────────────*  Department
AcademicYear 1────────────*  Division
Department   1────────────*  Course
Department   1────────────*  ClassRoom (owning dept)
Department   1────────────*  Division
Department   1────────────*  TeacherEntity (users)
Division     1────────────*  Batch
Division     1────────────*  Student
Division     1────────────*  TimetableEntry
TimeSlot     1────────────*  TimetableEntry
ClassRoom    1────────────*  TimetableEntry
CourseEntity 1────────────*  TimetableEntry
TeacherEntity 1───────────*  TimetableEntry   (as the "teacher")
TeacherEntity 1───────────*  TeacherAvailability
AcademicYear 1────────────*  TimetableEntry
LabSessionGroup 1─────────*  TimetableEntry

TeacherEntity *──────────* CourseEntity  (teacher_courses join table)
```

## 6.2 Table-by-Table Dictionary

### `academic_years`
| Column | Type | Purpose |
|--------|------|---------|
| `id` | BIGSERIAL PK | |
| `year_name` | VARCHAR(20) UNIQUE NOT NULL | e.g. "2024-25" |
| `start_date` | DATE NOT NULL | |
| `end_date` | DATE NOT NULL | |
| `is_current` | BOOLEAN DEFAULT false | Only one row should be true; enforced in service, not DB |
| `created_at` | TIMESTAMP | |

### `departments`
| Column | Type | Purpose |
|--------|------|---------|
| `id` | BIGSERIAL PK | |
| `name` | VARCHAR(100) NOT NULL | "Computer Engineering" |
| `code` | VARCHAR(10) NOT NULL | "COMP" |
| `head_of_department` | VARCHAR(100) | Textual HOD name (not a user FK) |
| `years` | VARCHAR | Comma-sep list, e.g. "1,2,3,4" |
| `academic_year_id` | FK → academic_years(id) | The year this dept belongs to |
| `created_at`, `updated_at` | TIMESTAMP | |

### `courses`
| Column | Type | Purpose |
|--------|------|---------|
| `id` | BIGSERIAL PK | |
| `name` | VARCHAR(100) NOT NULL | |
| `code` | VARCHAR(20) UNIQUE NOT NULL | |
| `course_type` | VARCHAR (enum THEORY/LAB) | |
| `credits` | INTEGER | |
| `hours_per_week` | INTEGER | |
| `semester` | VARCHAR (enum SEM_1..SEM_8) | |
| `description` | TEXT | |
| `prerequisites` | TEXT | |
| `year` | INTEGER | 1=FY, 2=SY, 3=TY, 4=BTech |
| `is_active` | BOOLEAN | |
| `department_id` | FK → departments | |

### `divisions`
| Column | Type | Purpose |
|--------|------|---------|
| `id` | BIGSERIAL PK | |
| `name` | VARCHAR(10) NOT NULL | "A", "B", "SY-A" |
| `year` | INTEGER CHECK (1..4) | |
| `branch` | VARCHAR(50) | |
| `total_students` | INTEGER DEFAULT 0 | Used for room capacity conflict |
| `is_active` | BOOLEAN | |
| `time_slot_type` | VARCHAR(20) DEFAULT 'TYPE_1' | Which time-slot profile |
| `class_teacher`, `class_representative` | VARCHAR(100) | |
| `department_id` | FK → departments | |
| `academic_year_id` | FK → academic_years | |

### `batches`
| Column | Type | Purpose |
|--------|------|---------|
| `id` | BIGSERIAL PK | |
| `name` | VARCHAR(50) NOT NULL | "A1", "A2", "A3" |
| `division_id` | FK → divisions | |

### `classrooms`
| Column | Type | Purpose |
|--------|------|---------|
| `id` | BIGSERIAL PK | |
| `name` | VARCHAR(50) UNIQUE NOT NULL | |
| `room_number` | VARCHAR(20) UNIQUE NOT NULL | e.g. "H202" |
| `building_wing` | VARCHAR(10) NOT NULL | "H" |
| `capacity` | INTEGER NOT NULL | |
| `room_type` | VARCHAR (enum CLASSROOM/LAB/AUDITORIUM) | |
| `has_projector`, `has_ac` | BOOLEAN | |
| `equipment` | TEXT | |
| `is_active` | BOOLEAN | |
| `department_id` | FK → departments (nullable) | Owning dept |

### `time_slots`
| Column | Type | Purpose |
|--------|------|---------|
| `id` | BIGSERIAL PK | |
| `start_time`, `end_time` | TIME NOT NULL | |
| `duration_minutes` | INTEGER NOT NULL | Used by the weekly-hour conflict check |
| `slot_name` | VARCHAR(50) | "Period 1", "Lunch Break" |
| `is_break` | BOOLEAN | If true, entries are rejected in this slot |
| `is_active` | BOOLEAN | |
| `type` | VARCHAR(20) DEFAULT 'TYPE_1' | Which template this slot belongs to |

### `users` (entity class is `TeacherEntity`)
| Column | Type | Purpose |
|--------|------|---------|
| `id` | BIGSERIAL PK | |
| `name` | VARCHAR(100) NOT NULL | |
| `employee_id` | VARCHAR(20) UNIQUE NOT NULL | |
| `email` | VARCHAR UNIQUE | |
| `phone` | VARCHAR(15) | |
| `min_weekly_hours` | INTEGER DEFAULT 10 | |
| `max_weekly_hours` | INTEGER DEFAULT 30 | |
| `is_first_login` | BOOLEAN DEFAULT true | |
| `specialization` | TEXT | |
| `is_active` | BOOLEAN DEFAULT true | Account usability |
| `is_approved` | BOOLEAN DEFAULT false | Admin approval |
| `is_email_verified` | BOOLEAN DEFAULT false | |
| `password` | VARCHAR NOT NULL | BCrypt hash |
| `role` | VARCHAR DEFAULT 'TEACHER' | ADMIN / HOD / TIMETABLE_COORDINATOR / TEACHER |
| `verification_token`, `verification_token_expiry` | | For email verify |
| `password_reset_token`, `password_reset_token_expiry` | | For forgot-password |
| `failed_login_attempts` | INTEGER DEFAULT 0 | |
| `account_locked_until` | TIMESTAMP | |
| `department_id` | FK → departments | |

### `teacher_courses` (join table)
| Column | Type |
|--------|------|
| `user_id` | FK → users |
| `course_id` | FK → courses |

Represents "which courses does this teacher teach". Many-to-many.

### `user_availability` (entity class is `TeacherAvailability`)
| Column | Type | Purpose |
|--------|------|---------|
| `id` | BIGSERIAL PK | |
| `user_id` | FK → users | |
| `day_of_week` | VARCHAR (enum) | |
| `start_time`, `end_time` | TIME | |
| `is_available` | BOOLEAN DEFAULT true | |

One row per (day, slot, available?) triple. Teachers can express "I'm unavailable Monday 3–5 pm" by inserting a row with `is_available=false`.

### `timetable_entries`
| Column | Type | Purpose |
|--------|------|---------|
| `id` | BIGSERIAL PK | |
| `division_id` | FK → divisions | |
| `course_id` | FK → courses | |
| `user_id` | FK → users | The teacher column is called `user_id`, not `teacher_id` — it references the users table. |
| `classroom_id` | FK → classrooms | Column is called `classroom_id`, not `room_id`. |
| `time_slot_id` | FK → time_slots | |
| `day_of_week` | VARCHAR (enum MONDAY..SATURDAY) | |
| `academic_year_id` | FK → academic_years | |
| `status` | VARCHAR (enum DRAFT/PUBLISHED/ARCHIVED) DEFAULT 'DRAFT' | |
| `semester` | VARCHAR (enum SEM_1..SEM_8) | Optional |
| `is_lab_session` | BOOLEAN DEFAULT false | |
| `batch_id` | FK → batches | Nullable — only for lab entries |
| `lab_session_group_id` | FK → lab_session_groups | Nullable — ties lab entries together |
| `week_number` | INTEGER DEFAULT 1 | Reserved — currently all entries are week 1 |
| `is_recurring` | BOOLEAN DEFAULT true | |
| `notes` | TEXT | |
| `created_at`, `updated_at` | TIMESTAMP | |

### `lab_session_groups`
| Column | Type | Purpose |
|--------|------|---------|
| `id` | BIGSERIAL PK | |
| `division_id` | FK | |
| `course_id` | FK | |
| `academic_year_id` | FK | |
| `day_of_week`, `time_slot_id` | | Starting slot of the 2-period session |
| `semester` | | |
| `created_by` | FK → users | Who initiated this lab session |

### `students`
| Column | Type | Purpose |
|--------|------|---------|
| `id` | BIGSERIAL PK | |
| `name`, `roll_number`, `email`, `phone` | | |
| `admission_year` | INTEGER | |
| `is_active` | BOOLEAN | |
| `division_id` | FK → divisions | |

Students are currently stored for reporting / capacity calculations; there is no student-facing login.

## 6.3 Important Integrity Rules

- **Unique**: `academic_years.year_name`, `departments.(name+academic_year_id)` (enforced by service), `courses.code`, `classrooms.name`, `classrooms.room_number`, `users.email`, `users.employee_id`, `students.roll_number`.
- **Cascade behaviour**: `AcademicYear → Division`, `Division → TimetableEntry` and `Division → Student`, `LabSessionGroup → TimetableEntry` all cascade on delete via JPA `CascadeType.ALL`. Deleting a division therefore wipes its schedule. Be careful.
- **Null rules**:
  - `classrooms.department_id` may be null (shared room). All other department FKs are required.
  - `timetable_entries.batch_id` and `lab_session_group_id` are null for theory classes.
  - `users.department_id` may be null only for ADMIN (other roles must have a department).

---

# 7. APIs & Integrations

## 7.1 Conventions

- Base URL in dev: `http://localhost:8083`
- All JSON both ways.
- Authentication: `Authorization: Bearer <jwt>` header on every non-`/auth/**` request.
- Conflict / validation errors: HTTP 409 with body `{ "message": "...", "conflicts": [...] }`.
- Generic errors: HTTP 400 with plain-text or simple JSON; the global exception handler masks stack traces.
- Access denied from `SecurityConfig`: HTTP 403 with body `{ "status": 403, "error": "AccessDenied" }` so the frontend can differentiate these from validation errors.

## 7.2 Public Endpoints (no auth)

| Method | Path | Body / Params | Purpose |
|--------|------|---------------|---------|
| POST | `/auth/login` | `{ email, password }` | Returns `{ email, token, role, firstLogin }` |
| POST | `/auth/register` | `{ name, employeeId, email, phone, specialization, password, departmentId? }` | Self-register |
| GET  | `/auth/verify-email?token=...` | | One-shot email verification |
| POST | `/auth/forgot-password` | `{ email }` | Sends reset link |
| POST | `/auth/reset-password` | `{ token, newPassword }` | Resets password |
| GET  | `/auth/reset-password?token=...` | | 302 redirect to frontend |
| POST | `/auth/change-first-password` | `{ email, newPassword }` | First-login change |

## 7.3 Authenticated (any logged-in user)

| Method | Path | Purpose |
|--------|------|---------|
| GET  | `/api/academic-years` | List all academic years |
| GET  | `/api/academic-years/current` | The current academic year |
| GET  | `/api/time-slots` | Public read of time slots |
| GET  | `/api/timetable/division/{divisionId}?academicYearId=...` | Published division timetable (cached) |
| GET  | `/api/timetable/teacher/{teacherId}?academicYearId=...` | Published teacher timetable (cached) |
| GET  | `/api/staff/profile` | Own profile |
| PUT  | `/api/staff/profile` | Update own phone/specialization |
| POST | `/api/staff/change-password` | `{ currentPassword, newPassword, confirmPassword }` |
| GET  | `/api/staff/availability` | Own availability rows |
| PUT  | `/api/staff/availability` | Replace all availability rows |
| GET  | `/api/teachers` (TEACHER/ADMIN) | List all teachers (limited by permission) |
| GET  | `/api/teachers/{id}` | Single teacher |

## 7.4 ADMIN / HOD / TIMETABLE_COORDINATOR

### Academic structure
| Method | Path | Purpose |
|--------|------|---------|
| GET/POST/PUT/DELETE | `/admin/api/academic-years[/{id}]` | CRUD |
| GET/POST/PUT/DELETE | `/admin/api/departments[/{id}]` | CRUD |
| GET | `/admin/api/departments/academic-year/{academicYearId}` | Filtered |
| POST | `/admin/api/departments/copy` | Copy a set of depts to a new year |
| GET/POST/PUT/DELETE | `/admin/api/courses[/{id}]` | CRUD |
| GET/POST/PUT/DELETE | `/admin/api/divisions[/{id}]` | CRUD |
| GET | `/admin/api/divisions/academic-year/{yearId}` | Filtered |
| GET/POST/PUT/DELETE | `/admin/api/rooms[/{id}]` | CRUD |
| GET/POST/PUT/DELETE | `/admin/api/time-slots[/{id}]` | CRUD |
| GET | `/admin/api/time-slots/type/{type}` | Filtered by TYPE_1/TYPE_2 |
| GET/POST/PUT/DELETE | `/admin/api/batches[/{id}]` | CRUD |
| GET | `/admin/api/batches/division/{divisionId}` | Filtered |

### Timetable (admin write)
| Method | Path | Purpose |
|--------|------|---------|
| GET | `/api/timetable/draft?divisionId=&academicYearId=` | Draft entries |
| POST | `/api/timetable/entries` | Add entry (conflict-checked) |
| PUT  | `/api/timetable/entries/{id}` | Update entry (also drag/drop) |
| DELETE | `/api/timetable/entries/{id}` | Remove |
| DELETE | `/api/timetable/draft?divisionId=...&academicYearId=...` | Clear all drafts |
| POST | `/api/timetable/lab-session` | Wizard: atomic N×2 creation |
| POST | `/api/timetable/lab-groups` | Lower-level group create |
| DELETE | `/api/timetable/lab-groups/{groupId}` | Cascade delete |
| POST | `/api/timetable/copy?sourceDivisionId=&targetDivisionId=&academicYearId=` | Copy drafts |
| GET | `/api/timetable/validate?divisionId=&academicYearId=` | Pre-publish dashboard |
| POST | `/api/timetable/publish?divisionId=&academicYearId=&force=` | Publish |
| POST | `/api/timetable/archive?divisionId=&academicYearId=` | Archive currently-published |

### Exports
| Method | Path | Response |
|--------|------|----------|
| GET | `/api/timetable/export/division/{id}/pdf?academicYearId=` | `application/pdf` |
| GET | `/api/timetable/export/division/{id}/excel?academicYearId=` | `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` |
| GET | `/api/timetable/export/teacher/{id}/pdf?academicYearId=` | PDF |
| GET | `/api/timetable/export/teacher/{id}/excel?academicYearId=` | Excel |

## 7.5 ADMIN / HOD only

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/admin/upload-staff` (multipart) | CSV staff upload |
| GET | `/admin/download-staff-template` | Template CSV |
| POST | `/admin/create-staff` | Single manual staff |
| PUT | `/admin/update-staff/{id}` | |
| POST | `/admin/upload-courses` (multipart, departmentId, year) | Bulk course CSV |
| GET | `/admin/download-courses-template` | Template |
| GET | `/api/teachers/pending-approvals` | Queue |
| POST | `/api/teachers/{id}/approve` | Approve |
| POST | `/api/teachers/{id}/reject` | Reject with reason |

## 7.6 Authentication Details

### Token shape
```
Header:  { "alg": "HS256" }
Payload: { "sub": "<email>", "iat": <epoch>, "exp": <epoch + 3600> }
```

### Token usage
- Stored in localStorage if `rememberMe=true`, else sessionStorage. Zustand writes to both during `login()` and the axios interceptor reads from either.
- Expiry = 1 hour. No refresh token — user must re-login.
- Rotation is possible via logout + new login (server has no revocation list).

### CORS
- Allowed origins set via `app.cors.allowed-origins` (comma-separated).
- Allowed methods: GET, POST, PUT, DELETE, PATCH, OPTIONS.
- Allowed headers: Content-Type, Authorization, Accept, X-Requested-With.
- Credentials allowed (cookies not used, but header can still travel).

## 7.7 External Integrations

| Integration | Purpose | Where |
|-------------|---------|-------|
| **Gmail SMTP** | Email verification, welcome, approval, rejection, password reset | `EmailService`; env vars `EMAIL_USERNAME`, `EMAIL_PASSWORD` (Gmail app password) |
| **Redis** (Memurai on Windows) | Cache for published timetables; connection pool configured via `spring.data.redis.*` | `RedisConfig` |
| **PostgreSQL** | Primary database | Connection string in `application-dev.properties` |
| **AWS RDS** (prod) | Managed PostgreSQL | Env var `SPRING_DATASOURCE_URL` |
| **AWS Amplify** | Frontend hosting + build | `amplify.yml` |
| **AWS EC2 + ALB** | Backend hosting | Scripts in `AWS deployment files/` |
| **GitHub Actions** | CI (always on), deploy (manual) | `.github/workflows/*.yml` |

---

# 8. Setup & Installation Guide

## 8.1 Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Java JDK | 17 | Lombok does NOT support Java 25. Use 17. |
| Maven | 3.6+ | `mvnw` wrapper included, but a global Maven is easier. |
| Node.js | 18+ | With npm 9+. |
| PostgreSQL | 17 | Or 14+ works. |
| Redis | any 6+ | On Windows, install **Memurai** (drop-in Redis replacement). |
| Git | 2.30+ | |

Verify installs:

```
java -version                 # openjdk 17.x
mvn -v                        # Apache Maven 3.9.x
node -v                       # v18.x or newer
psql --version                # psql (PostgreSQL) 17.x
redis-cli ping                # PONG  (or Memurai)
```

## 8.2 Clone and Inspect

```
git clone https://github.com/wani21/SamaySetu_Connect_plan_manage.git
cd SamaySetu_Connect_plan_manage
```

Key top-level files to glance at: `README.md`, `SAMAYSETU_PROJECT_BIBLE.md` (this doc), `Scripts/seed_data.sql`.

## 8.3 Database Setup

1. **Create an empty database** named `SamaySetu` (case-sensitive).
   ```
   createdb -U postgres SamaySetu
   ```
   Or via `pgAdmin` → right-click Databases → Create → Database → name `SamaySetu`.

2. **Default credentials** expected by `application-dev.properties`:
   - host `localhost`, port `5432`
   - user `postgres`, password `changeme`
   - override by exporting `SPRING_DATASOURCE_PASSWORD=...` before starting the backend.

3. **Let Hibernate create the schema on first backend boot** (ddl-auto=update creates/updates tables automatically).

4. **Seed the database** with `Scripts/seed_data.sql`:
   ```
   psql -U postgres -d SamaySetu -f Scripts/seed_data.sql
   ```
   The seed creates 2 academic years, 8 departments, 10 divisions, 32 courses, 16 rooms, 10 time slots, 21 teachers, 12 batches, 10 students, 32 course-teacher links.

## 8.4 Redis Setup

### Windows (recommended: Memurai)
1. Download Memurai Developer from `https://www.memurai.com`.
2. Install with defaults — it registers as a Windows service on `localhost:6379`.
3. `redis-cli ping` → `PONG`.

### macOS
```
brew install redis
brew services start redis
```

### Linux
```
sudo apt install redis-server
sudo systemctl start redis
```

## 8.5 Backend

```
cd Backend
# Windows
set SPRING_PROFILES_ACTIVE=dev
mvn spring-boot:run

# Unix/macOS
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run
```

Expected output:
```
...Started samaysetuApplication in 6.2 seconds
Security configuration initialized with allowed origins:
Default admin user created!
```

The backend runs on `http://localhost:8083`. Default admin credentials: `suryankadmin@mitaoe.ac.in` / `Admin@123`.

### Common boot failures

- **`Illegal base64 character '-'`** — the JWT secret must be a valid Base64 string. The default shipped in `application-dev.properties` is valid. If you overrode `JWT_SECRET_KEY` with something containing `-` or `_`, use plain Base64 only.
- **`Connection refused: localhost:5432`** — PostgreSQL isn't running or the database doesn't exist.
- **`Unable to connect to Redis`** — Memurai service not started.
- **Lombok compile errors (100+)** — you're on Java 25 or 21. Use Java 17.

## 8.6 Frontend

```
cd Frontend
npm install
npm run dev
```

Opens on `http://localhost:5173`. The dev server has hot reload. For a production build:

```
npm run build     # output in dist/
npm run preview   # preview the built bundle on :4173
```

## 8.7 First Login

1. Open `http://localhost:5173/login`.
2. Email: `suryankadmin@mitaoe.ac.in`, password: `Admin@123`.
3. Land on `/admin/dashboard`.
4. Sidebar → Timetable → pick academic year → pick division → start adding entries.

For testing as a teacher: the seed script creates 21 teacher accounts all using the dev password `mitaoe@123`. Pick any one from the database (`SELECT email FROM users WHERE role='TEACHER' LIMIT 5;`).

## 8.8 Environment Variables

### Dev (optional overrides)
- `SPRING_DATASOURCE_PASSWORD` — if your local postgres password isn't `changeme`
- `EMAIL_USERNAME`, `EMAIL_PASSWORD` — if you want real email sending (Gmail app password)

### Prod (all required — no defaults in `application-prod.properties`)
```
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:5432/samaysetu
SPRING_DATASOURCE_USERNAME=...
SPRING_DATASOURCE_PASSWORD=...
JWT_SECRET_KEY=<base64-encoded 256-bit key>
EMAIL_USERNAME=...
EMAIL_PASSWORD=...
APP_BASE_URL=https://api.yourdomain.com
APP_FRONTEND_URL=https://yourapp.amplifyapp.com
APP_CORS_ALLOWED_ORIGINS=https://yourapp.amplifyapp.com
APP_ADMIN_EMAIL=admin@mitaoe.ac.in
APP_ADMIN_PASSWORD=<strong-random-password>
REDIS_HOST=...
REDIS_PORT=6379
REDIS_PASSWORD=...
```

Frontend prod env (set in AWS Amplify Console build settings):
```
VITE_API_URL=https://api.yourdomain.com
```

---

# 9. Current Implementation Status

## 9.1 Completed ✓

### Academic Structure
- ✓ Academic Year CRUD with `isCurrent` invariant
- ✓ Department CRUD, scoped per academic year, copy-to-new-year action
- ✓ Course CRUD + bulk CSV upload + template download
- ✓ Division CRUD with student strength, class teacher, class rep, `timeSlotType`
- ✓ Batch CRUD filtered by division
- ✓ Room CRUD with type + capacity + wing + department
- ✓ Time Slot CRUD with TYPE_1 / TYPE_2 templates and break flagging

### Staff
- ✓ CSV staff upload with quote-aware parsing
- ✓ Manual staff creation with role selection
- ✓ Staff update by admin
- ✓ Teacher self-registration with MITAOE email enforcement
- ✓ Email verification flow
- ✓ Admin/HOD approval queue + approve/reject with email notification
- ✓ First-login password change
- ✓ Password change from within app
- ✓ Forgot-password / reset-password email flow
- ✓ Account lockout after failed attempts

### Timetable Builder
- ✓ Grid view with day columns, time-slot rows
- ✓ Type-1/Type-2 slot filtering per division
- ✓ Break rows rendered as merged grey banners
- ✓ Click-to-add, click-to-edit, delete
- ✓ Drag-and-drop move + swap
- ✓ Lab Session Wizard (atomic N×2 entries with shared LabSessionGroup)
- ✓ Copy from division
- ✓ Clear draft
- ✓ Archive published
- ✓ 8-point conflict detection with human-readable messages
- ✓ Pre-publish validation dashboard with errors + warnings
- ✓ Publish with cache eviction + automatic previous-archive

### Exports
- ✓ PDF with MITAOE header, merged break rows, purple lab cells
- ✓ Excel .xlsx with merged breaks, styled lab cells, auto-sized columns
- ✓ Both division and teacher variants

### Teacher-side
- ✓ Personal weekly timetable (read-only)
- ✓ Profile view + limited edit
- ✓ Password change
- ✓ Weekly availability editor (atomic save)

### Security
- ✓ Spring Security with JWT
- ✓ BCrypt password hashing
- ✓ Role-based authorization via URL matchers + `@PreAuthorize`
- ✓ Rate limiting on auth endpoints
- ✓ XSS sanitisation on request bodies
- ✓ Security headers (HSTS, X-Frame-Options, CSP, Cache-Control)
- ✓ Audit logs for admin actions
- ✓ Structured 401/403 responses so the frontend can distinguish from validation errors

### Deployment
- ✓ Dev/prod profile split with no secrets in prod defaults
- ✓ CI via GitHub Actions (always-on build+test)
- ✓ AWS EC2 + ALB scripts (manual trigger)
- ✓ AWS Amplify frontend build pipeline

## 9.2 Partially Done / In-Flight

- **Student-facing view** — `students` table exists, but there is no student login or read-only student dashboard. Published PDFs are the only student-visible artifact.
- **Flyway migrations** — wired in `pom.xml` but disabled; schema is still managed by Hibernate `ddl-auto=update`. Production switch is documented but not executed.
- **Notifications for teachers** — when a timetable is published, teachers aren't actively notified. They have to log in to see changes.
- **Audit log retention / viewer UI** — logs go to stdout; there's no admin UI to browse or search.
- **Course-to-semester auto-linking** — courses have a `semester` enum but the UI doesn't auto-suggest courses when building a given semester's timetable.
- **Bulk time-slot creation** — admin still creates each slot individually; a "preset TYPE_1 / TYPE_2" button would speed onboarding.

## 9.3 Known Limitations / Bugs

- **Single timetable grid variant** — a division has one published timetable per academic year. If an admin wants to experiment with two candidate layouts, they have to do it outside the system or rely on the DRAFT/PUBLISHED distinction.
- **No in-app messaging** — approvals, rejections, and password resets use email. If email is misconfigured, the user is stuck. A fallback SMS/WhatsApp is not implemented.
- **No mobile-first timetable builder** — the grid works on desktop; on mobile it's horizontally scrollable but drag-and-drop is hit-or-miss on touch devices (dnd-kit supports touch but the grid's cell sizes are small).
- **Conflict check `excludeId` via PUT edit** — moving an entry via drag-and-drop PUT works because `excludeId` is the entry ID. But rapid double-drag can race: one PUT lands before the other; worst case is that the second gets a stale conflict and the UI rolls back.
- **Email dispatch is synchronous** — send fails block the registration request. Should be async (e.g. `@Async` with a dedicated executor) so a Gmail hiccup doesn't kill user signup.
- **`TeacherCred` entity** — a leftover from an early design, kept for schema compatibility; nothing in the service layer actually uses it.
- **Duplicate import in `TimetableController`** — `CreateLabSessionGroupDTO` is imported twice at top of file. Harmless, but reviewers notice.
- **No soft-delete for entities** — `DELETE` is hard-delete. If a division is deleted, all its timetable entries go with it. Cascade is intentional but irreversible.

## 9.4 Recent Major Changes

- **2026-04-23** — JWT/403 redirect loop fixed. The frontend Axios interceptor was matching the substring `"forbidden"` in response bodies and forcing logout on any 403. Fixed by:
  (1) backend returns structured `{ "error": "AccessDenied" }` JSON instead of plain-text containing "Forbidden";
  (2) frontend only triggers logout if the response body's `error` field equals `AccessDenied` or `AuthRequired`;
  (3) frontend adds `isRedirectingToLogin` guard so concurrent 401s only redirect once;
  (4) `TeacherService.loadUserByUsername` now normalises `role` to uppercase so `hasRole("ADMIN")` matches regardless of DB casing.
- **2026-04-14** — Renamed "Staff" to "Faculty" across the frontend UI. (Note: the current checked-out branch has partially reverted this; both terms appear.)
- **2026-04-09** — Enterprise overhaul: drag-and-drop, 8-point conflict engine, PDF/Excel export, roles (HOD, TIMETABLE_COORDINATOR), rate limiting, audit logs, XSS sanitisation.
- **2026-04-09** — Fix allowing `java.sql.Timestamp` in Redis polymorphic type validator (so `createdAt`/`updatedAt` survive the round-trip through the cache).

---

# 10. Future Scope & Roadmap

## 10.1 Near-Term (1-month horizon)

1. **Course dropdown filtered by selected division's department** — when building COMP SY-A's timetable, the course dropdown currently lists every course in the database. Filtering by `division.department.id` would cut dropdown length by 5–10×. (Backend: no change; frontend: filter `courses` by `course.department.id === division.department.id`.)
2. **Room dropdown filtered by selected division's department** — same rationale; labs in particular should only be offered if they are shared or owned.
3. **Admin view of teachers + individual teacher timetables** — a "Faculty Timetables" page that groups by department, lists every teacher with their published weekly timetable one click away. Backend endpoints already exist (`/api/timetable/teacher/{id}/...`); needs a new frontend page.
4. **Shared-lab support via `ClassRoom.allowedDepartments`** — a many-to-many relation so a Hardware Lab owned by Mechanical can also appear in Civil's timetable builder. Backend entity change + UI checkbox grid in the Rooms page.
5. **Teacher load distribution** — a `TeacherCourseLoad` entity with `(teacherId, courseId, allocatedHours)`. Admin enters "Prof. X teaches Java for 4 hrs/week, ML for 3 hrs/week". Pre-publish validation can then warn if the scheduled hours don't match the allocated hours.
6. **Draft export** — currently exports only PUBLISHED entries; adding `?status=DRAFT` to export endpoints lets admins print their work-in-progress before publishing.
7. **Flyway migration baseline** — capture current schema via `pg_dump --schema-only`, commit as `V1__baseline.sql`, flip `spring.flyway.enabled=true` and `ddl-auto=validate` in prod.
8. **Async email dispatch** — move `EmailService` calls into `@Async` methods with a thread pool so user-facing flows don't block on SMTP latency.

## 10.2 Medium-Term

1. **Student read-only portal** — a simple `/student/timetable` page that picks roll number and academic year and shows the weekly grid. Or even deep-link URLs that a class rep can WhatsApp to classmates.
2. **In-app notifications** — bell icon with a list of "your timetable was updated", "your leave request was approved", etc. Requires a notification table + WebSocket or polling.
3. **Bulk time-slot presets** — one-click "apply TYPE_1 default" that creates 10 time slots at once.
4. **Room-course type enforcement** — currently just a warning. Optional strict mode that blocks LAB course in CLASSROOM room.
5. **Academic calendar support** — holidays, exam weeks, special events that change the Monday–Saturday rhythm.
6. **Constraint-based auto-generation** — given divisions + courses + teachers + constraints, produce a draft timetable automatically (CSP / OR-Tools solver). Currently everything is manual; an auto-generate button would be a huge productivity win.
7. **Mobile app** — React Native reuse of component library for a read-only teacher/student view.

## 10.3 Long-Term / Strategic

1. **Multi-tenant** — generalise from MITAOE-only to any college. Needs tenant scoping on every table, a tenant-picker on login, and a landing page for new colleges.
2. **AI-assisted scheduling** — propose swaps when a teacher calls in sick ("Prof. A is out today — here are three swap options that respect every constraint").
3. **Analytics** — which rooms are underutilised? Which teachers are overloaded? How often do conflicts happen on which day of the week?
4. **Webhooks / ICS export** — give teachers a calendar subscription URL so Google Calendar / Outlook reflect their timetable live.
5. **Attendance integration** — tie to biometric or QR-code attendance so a class's attendance ticks the corresponding entry.

## 10.4 Scalability Considerations

- **Current capacity**: one EC2 instance (t3.medium) comfortably serves MITAOE's ~3000 students + 150 faculty. Reads go to Redis cache.
- **Scaling out reads**: add a Redis replica; scale the ASG horizontally. JWTs are stateless so there's no sticky-session concern.
- **Scaling out writes**: a single Postgres writer is more than enough at this scale. For a SaaS multi-tenant expansion, partition by tenant on Postgres or move to Citus.
- **Large CSV uploads**: the 5 MB cap (`spring.servlet.multipart.max-file-size=5MB`) keeps memory use bounded. For larger colleges, raise to 20 MB and stream row-by-row.

---

# 11. Developer Notes & Best Practices

## 11.1 Coding Standards

### Java
- **Package layout**: By layer (`Controller`, `Service`, `Repository`, `Entity`, `IO`, `Util`, `Configuration`, `Filter`, `Exception`) — easier to navigate than by domain for a project of this size.
- **Lombok**: Use `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor` on entities and DTOs. Use `@RequiredArgsConstructor` for services (constructor injection — Spring treats `final` fields as required deps).
- **Validation**: Always annotate DTO fields with Jakarta Validation annotations (`@NotBlank`, `@NotNull`, `@Email`, `@Size`, `@Min`, `@Max`). Request bodies must be `@Valid`.
- **Exception handling**: throw `ResponseStatusException` or a domain-specific `RuntimeException`. Don't return error strings directly from controllers — let `GlobalExceptionHandler` shape the response.
- **Logs**: use `LoggerFactory.getLogger(MyClass.class)`, not `System.out`. Prefix with domain tags: `[LOGIN]`, `[JWT]`, `[AUDIT]`, `[SEC]`.
- **Transactions**: use class-level `@Transactional(readOnly = true)` on services that are mostly reads, override with method-level `@Transactional` on writes.
- **Cache annotations**: `@Cacheable` on the read path, `@CacheEvict` on any write that can invalidate the cache.
- **Jackson**: Use `@JsonIgnoreProperties({ ... })` and `@JsonIgnore` on back-references to prevent infinite recursion during serialization.

### TypeScript / React
- **Strict mode**: `tsconfig.json` has `strict: true`. Do not loosen.
- **Functional components only**: no class components.
- **Hooks**: use `useState`, `useEffect`, `useMemo`, `useCallback` idiomatically. Dependency arrays must be complete (enable ESLint rule).
- **API calls**: always through `services/api.ts`. Never invoke `fetch` or `axios.get` inline in a component.
- **Error handling**: use `getErrorMessage(err)` from `utils/errorHandler.ts` when showing toasts.
- **Styling**: Tailwind utility classes. Avoid inline `style={}` except for dynamic values (e.g. drag transforms).
- **File naming**: PascalCase for components (`TimetableManagementPage.tsx`), camelCase for utility modules.

## 11.2 Important Assumptions

1. **One academic year is current** — `isCurrent=true` holds for exactly one row in `academic_years`. The `AcademicService.update` method enforces this by unsetting others on save.
2. **Emails are case-insensitive** — `users.email` is compared case-insensitively in login, but stored as entered. Avoid mixed-case insertion.
3. **The admin account is seeded on first boot** — `DataInitializer` creates it. Deleting `suryankadmin@mitaoe.ac.in` from the DB and restarting will recreate it unless prod credentials are set.
4. **A teacher's `role` string is one of `ADMIN`, `HOD`, `TIMETABLE_COORDINATOR`, `TEACHER`** — case-sensitive. `TeacherService` normalises to uppercase on auth, but DB inserts must also use uppercase to prevent drift.
5. **Divisions belong to an academic year, not a department alone** — querying "all divisions" without filtering by year will show past years too.
6. **The `TimeSlot.type` column** — `TYPE_1` and `TYPE_2` are the only values in current use. A future `TYPE_3` (e.g. a compressed weekend schedule) would require no code changes, just data.
7. **Frontend JWT storage key is `jwt_token`** — the key name is hardcoded in multiple places (`store/authStore.ts`, `services/api.ts`). Change with care.
8. **Redis is available in all environments** — there is no "cache-free" fallback. If Redis goes down, `@Cacheable` methods will error out. For dev, start Memurai / Redis before the backend.
9. **Email sending is non-fatal for some flows** — registration succeeds even if the verification email fails (logged as warning). Forgot-password also degrades gracefully.
10. **Lab session group entries share the same time slot and day but have different (batch, teacher, room)** — the conflict service explicitly exempts lab entries from teacher/division uniqueness so parallel batches work.

## 11.3 Common Pitfalls

### "I added a field to the entity but it's not saving"
- Check that `ddl-auto=update` is on (dev profile). Check Hibernate logs for the ALTER statement.
- Check that the new field has a Lombok getter/setter (present if the class has `@Data`).
- Check that the DTO carries the new field and you're copying it into the entity.

### "My JWT works for login but /admin/api/... returns 403"
- Verify the `role` in DB is exactly `ADMIN` (uppercase). `TeacherService` normalises it, but if you're inspecting the SecurityContext authorities, they should read `[ROLE_ADMIN]`.
- Restart the backend after changing `role` directly in DB — the currently-held JWT in the browser still maps to whatever role was loaded at login time (in effect until the 1-hour expiry).

### "The timetable grid is empty even though I see rows in timetable_entries"
- The grid reads `DRAFT` entries. Run `SELECT status, COUNT(*) FROM timetable_entries GROUP BY status;`. If everything is `PUBLISHED`, the builder shows nothing — you need to clear the draft and start fresh or un-publish.
- Or: the selected academic year doesn't match the entries' `academic_year_id`.
- Or: the division's `timeSlotType` doesn't match the time slots' `type` (frontend filters silently).

### "The drag-and-drop doesn't work on mobile"
- `dnd-kit` activates on 8px drag threshold. Small screens + imprecise taps can fail to trigger. Use the + button to add instead.
- For lab-group entries, dragging is intentionally disabled (`disabled: hasLabGroup`).

### "Staff CSV upload returns 'column count mismatch'"
- Check you have the correct column order: `Name, Employee ID, Email, Phone, Specialization, Min Weekly Hours, Max Weekly Hours, Role(optional)`.
- Commas inside fields? Wrap the field in double quotes.

### "Redirected to /login over and over after successful login"
- Race condition between authStore hydration and route guard? Check `_hasHydrated` flag is true before rendering protected routes. `App.tsx` already has a hydration gate.
- Response interceptor matching "forbidden" / "token" in an error message and triggering `forceLogout`? This was the bug fixed on 2026-04-23 — make sure your backend returns structured JSON error bodies, not plain-text.

### "Password reset email never arrives"
- Check backend logs for `"Failed to send password reset email"`.
- Verify `EMAIL_USERNAME` is a Gmail address, `EMAIL_PASSWORD` is a Gmail app password (not your normal Google password), and 2FA is enabled on that Gmail account.
- In dev without real SMTP, substitute MailHog: `docker run -p 1025:1025 -p 8025:8025 mailhog/mailhog` then point the backend at `localhost:1025`.

### "The JWT throws 'Illegal base64 character'"
- The secret contains a hyphen or underscore. Base64 allows only `[A-Za-z0-9+/=]`. Re-encode with `echo -n 'my plain text key' | base64`.

## 11.4 Testing Tips

- **Postman collection** — `SamaySetu_Postman_Collection.json` at repo root. Import with `SamaySetu_Postman_Environment.json` to get pre-wired variables for dev.
- **Seed data** — re-run `Scripts/seed_data.sql` whenever you want a known state. Use `TRUNCATE ... RESTART IDENTITY CASCADE` at the top to clear everything first.
- **Quick smoke**: `GET http://localhost:8083/actuator/health` → `{"status":"UP"}`.
- **Auth smoke**:
  ```
  curl -X POST http://localhost:8083/auth/login \
    -H 'Content-Type: application/json' \
    -d '{"email":"suryankadmin@mitaoe.ac.in","password":"Admin@123"}'
  ```
  Response includes the `token`. Paste it into a subsequent `Authorization: Bearer ...` header.
- **Frontend dev tools**:
  - DevTools → Application → Local Storage → `jwt_token` (if rememberMe), otherwise Session Storage.
  - DevTools → Network → Filter by `Fetch/XHR` → see request headers for `Authorization`.

## 11.5 Deployment Checklist (Production)

- [ ] `SPRING_PROFILES_ACTIVE=prod` set in systemd / EC2 user data
- [ ] All prod env vars set (§8.8)
- [ ] `JWT_SECRET_KEY` is at least 32 bytes, Base64-encoded, unique per environment
- [ ] `EMAIL_PASSWORD` is a Gmail *app password*, not the account password
- [ ] Postgres user has only the privileges needed (SELECT/INSERT/UPDATE/DELETE on the SamaySetu DB; no SUPERUSER)
- [ ] `APP_CORS_ALLOWED_ORIGINS` contains only the frontend domain
- [ ] Redis requires auth (`requirepass`) and uses TLS in transit if available
- [ ] Audit logs are being forwarded somewhere permanent (CloudWatch, ELK, etc.)
- [ ] Database backups scheduled (RDS automated snapshots, at least 7-day retention)
- [ ] ALB health check points at `/actuator/health`
- [ ] Fresh admin account created with a strong password (not `Admin@123`)
- [ ] Flyway migrations committed (once you switch off ddl-auto=update)

## 11.6 Contribution Workflow

1. Branch off `main`: `git checkout -b feat/<short-description>`.
2. Make changes. Run `mvn clean compile` and `npm run build` locally — both must succeed.
3. Commit with a clear message:
   ```
   feat: <what-and-why>

   Bullet the rationale. Mention any breaking changes or migration steps.
   ```
4. Push and open a PR into `main`. GitHub Actions runs CI automatically.
5. After review, merge into `main`. For a deploy, manually trigger `deploy-backend.yml` from the Actions tab.

Absolutely do **not**:
- Commit secrets (`.env`, credentials, JWT keys).
- Use `--no-verify` on git hooks.
- Force-push to `main`.
- Add `console.log` or `System.out.println` to production code paths.

---

# Appendix A — Glossary

| Term | Meaning |
|------|---------|
| **Academic Year** | A one-year slot (e.g. 2025-26). The main "tenant" scope. |
| **Admin** | Highest-privilege role. Full system access. |
| **ASG** | Auto Scaling Group (AWS EC2 concept). |
| **Batch** | A sub-group of a division for lab sessions (A1, A2, A3). |
| **BCrypt** | Slow password hash algorithm with per-password salt. |
| **Break slot** | A time slot flagged `isBreak=true` — classes cannot be scheduled there. |
| **CORS** | Cross-Origin Resource Sharing. Allows the frontend domain to call the backend domain. |
| **Division** | A section of a year in a department (e.g. COMP SY-A). |
| **DRAFT / PUBLISHED / ARCHIVED** | The three states of a timetable entry. Only PUBLISHED is visible to non-admins. |
| **HOD** | Head of Department. Second-highest role. |
| **JWT** | JSON Web Token. Self-contained auth credential. |
| **Lab Session Group** | A parent row linking N×2 timetable entries that together form one lab session. |
| **MITAOE** | MIT Academy of Engineering, Alandi, Pune. |
| **Semester** | SEM_1 through SEM_8. Year 1 runs SEM_1+SEM_2, etc. |
| **Time Slot Type** | `TYPE_1` or `TYPE_2` — two possible daily rhythm templates. |
| **TIMETABLE_COORDINATOR** | A role for faculty volunteers who help build timetables but cannot approve other teachers. |

---

# Appendix B — End-to-End Walkthrough (Day in the Life)

### 08:30 — Admin logs in
Admin opens `https://samaysetu.mitaoe.ac.in`, logs in as `suryankadmin@mitaoe.ac.in`. Sees dashboard with "2 pending approvals". Clicks → approves both self-registered teachers.

### 09:00 — New semester starts
Admin clicks **Academic Structure → Academic Years → + Add**, creates `2025-26`, marks current.

### 09:10 — Departments
Goes to **Departments**. Clicks **+ Copy from previous year** to bring over 2024-25's department list.

### 09:15 — Onboarding the first new teacher
A new teacher was approved earlier. They log in for the first time, are redirected to `/change-first-password`, change password, land on their dashboard. Timetable is empty because no entries exist yet. They set availability for Monday afternoon (unavailable 15:00–17:00 — Research Council meeting).

### 09:30 — Building COMP SY-A
Admin goes to **Timetable**, picks year 2025-26, picks division COMP SY-A, picks semester SEM_3.
- Drags "Data Structures" onto Monday 9:00. Backend: conflict check, no conflicts, saves DRAFT.
- Clicks Monday 10:00 → Add Entry → picks "Operating Systems", Prof. Kulkarni, H201. Conflict check says "Room conflict: H201 already booked on Monday at this time slot." Fixes to H202.
- Continues filling the grid.

### 10:30 — Lab session
Clicks **+ Lab Session**. Picks "DBMS Lab", Monday 14:00 (lab runs 14:00–16:00 = periods 5+6).
Sees 3 batch rows (A1, A2, A3). Assigns Prof. Gadkari to A1 in B101, Prof. Sharma to A2 in B102, Prof. Patil to A3 in B103.
Backend checks conflicts for each batch assignment separately and creates 3×2=6 entries with a single `lab_session_group_id`. Saves.

### 11:15 — Copy to COMP SY-B
Picks COMP SY-B. Clicks **Copy from Division**, picks COMP SY-A. In one click, 20+ theory entries land in COMP SY-B's draft (labs intentionally skipped). Admin tweaks the teacher assignments where needed.

### 12:00 — Validate before publishing
Clicks **Publish** on COMP SY-A. The validation modal shows 1 error: "Prof. Kulkarni scheduled for 32 hours/week, limit is 30." Admin shrinks Kulkarni's Thursday load, retries — modal shows 0 errors, 2 warnings (Saturday empty, SY-A has 0 entries on Friday 17:00). Accepts warnings, clicks Publish.

### 12:05 — Teacher sees the change
Prof. Kulkarni refreshes their timetable. Redis cache was evicted at publish; the new fetch returns the updated schedule. Kulkarni downloads a PDF, forwards to WhatsApp.

### 14:00 — Student asks "where is my class?"
A CR shares the division PDF with the class WhatsApp group. Students see Monday 09:00 = Data Structures, H202.

### 16:00 — A teacher calls in sick
Admin logs in, goes to Timetable, picks the affected division. Edits Prof. Patil's entry: changes teacher to Prof. Phadke (substitute). Conflict check warns "Phadke has 31 hours/week projected." Admin accepts and saves.

### 17:00 — End-of-day audit
Admin checks backend logs: `[AUDIT]` lines show approvals, publishes, and edits. All expected.

---

# Appendix C — Troubleshooting Guide

| Symptom | Likely Cause | Fix |
|---------|--------------|-----|
| Backend fails to start with `Connection refused` | Postgres or Redis not running | Start both, verify with `psql` and `redis-cli ping` |
| Backend fails with `Illegal base64 character` | JWT secret contains hyphens/underscores | Re-encode secret as plain Base64 |
| Lombok compile errors flood | Java version is 21+ | Install JDK 17, set JAVA_HOME |
| Login returns 500 | Check server logs for `[LOGIN]` trace | Fix the revealed cause (password not BCrypt? role missing?) |
| Login succeeds but fetches return 403 | Role in DB is lowercase / mis-cased | `UPDATE users SET role='ADMIN' WHERE email='...'` |
| Frontend redirect loop `/login ↔ /admin` | Old-style 403 body triggering interceptor | Restart backend with current code; 403 now returns JSON `{ "error": "AccessDenied" }` |
| 429 Too Many Requests | Rate limit hit on `/auth/**` | Wait 60 seconds; raise limit via `app.rate-limit.max-requests` |
| `application/octet-stream` PDF on download | Axios default MIME for blob | Response already sets `Content-Type: application/pdf`; confirm frontend `triggerDownload` passes correct MIME |
| Staff password `mitaoe@123` doesn't work | Running `SPRING_PROFILES_ACTIVE=prod` by accident | Set `SPRING_PROFILES_ACTIVE=dev` for local use |
| Can't log in as seeded teacher | `is_active=false` or `is_approved=false` in DB | `UPDATE users SET is_active=true, is_approved=true, is_email_verified=true WHERE email='...'` |
| Redis cache stale after publish | `@CacheEvict` failed silently | Manually `redis-cli FLUSHDB` as a quick fix; investigate cache-evict call paths |
| Postgres `year_name already exists` on seed re-run | `academic_years` unique constraint | Seed script should `TRUNCATE ... RESTART IDENTITY CASCADE` before inserts |
| Frontend shows "Failed to load data" after login | Axios failing on `/admin/api/departments` | Open DevTools Network → check status code → consult 401/403 flows above |

---

## Closing Note

This document is the single source of truth for the SamaySetu project as of the date at the top.

If something in the code disagrees with this document, the **code wins** — but open an issue or PR to update this document so the next person learns from the correction, not in spite of it.

**Built with care for MIT Academy of Engineering, Alandi, Pune, Maharashtra.**

*— The SamaySetu Team*
