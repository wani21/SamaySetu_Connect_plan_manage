# SamaySetu - Smart Timetable Management System

[![MIT License](https://img.shields.io/badge/License-MIT-green.svg)](https://choosealicense.com/licenses/mit/)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.0+-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18+-blue.svg)](https://reactjs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.0+-blue.svg)](https://www.typescriptlang.org/)

> A comprehensive timetable management system designed to streamline academic scheduling and resource allocation for educational institutions.

## 🎯 Overview

SamaySetu is a modern, web-based timetable management system built specifically for colleges and universities. It provides an intuitive interface for administrators to manage academic structures, staff, and schedules while offering teachers and students easy access to their timetables.

## ✨ Key Features

### 🔐 Authentication & Authorization
- **Secure Login System** with JWT-based authentication
- **Role-based Access Control** (Admin, Teacher, Student)
- **Email Verification** for account activation
- **Password Reset** functionality
- **First-time Login** password change requirement

### 👨‍💼 Administrative Features
- **Academic Year Management** - Create and manage multiple academic years
- **Department Structure** - Organize departments by academic years with year-specific availability
- **Staff Management** - Bulk Registration via CSV or manual entry with approval workflow
- **Course Management** - Create courses with semester-wise organization
- **Division & Batch Management** - Organize students into divisions and batches
- **Time Slot Configuration** - Flexible scheduling with multiple time slot types
- **Room Management** - Track and assign classrooms with building/wing organization

### 📚 Academic Structure
- **Hierarchical Organization**: Academic Years → Departments → Years (FY/SY/TY/BTech) → Divisions → Batches
- **Semester-wise Course Management** (8 semesters supported)
- **Department Copying** between academic years
- **Class Teacher & CR Assignment** for divisions
- **Multiple Schedule Types** (Schedule 1 & Schedule 2) for flexible timing

### 📊 Data Management
- **CSV Import/Export** for bulk operations
- **Template Downloads** for standardized data entry
- **Comprehensive Error Handling** with detailed validation messages
- **Real-time Data Synchronization**

## 🏗️ System Architecture

### Backend (Spring Boot)
```
├── Controllers/     # REST API endpoints
├── Services/        # Business logic layer
├── Repositories/    # Data access layer
├── Entities/        # JPA entity models
├── Security/        # Authentication & authorization
└── Utils/          # Helper utilities
```

### Frontend (React + TypeScript)
```
├── components/      # Reusable UI components
├── pages/          # Application pages
├── services/       # API integration
├── utils/          # Helper functions
└── types/          # TypeScript definitions
```

## 🚀 Getting Started

### Prerequisites
- **Java 17+**
- **Node.js 18+**
- **MySQL 8.0+**
- **Maven 3.6+**

### Backend Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/mitaoe/SamaySetu.git
   cd SamaySetu
   ```

2. **Configure Database**
   ```bash
   # Create MySQL database
   mysql -u root -p
   CREATE DATABASE samaysetu_db;
   ```

3. **Update Application Properties**
   ```properties
   # Backend/src/main/resources/application.properties
   spring.datasource.url=jdbc:mysql://localhost:3306/samaysetu_db
   spring.datasource.username=samaysetu_user
   spring.datasource.password=samaysetu_password
   ```

4. **Run Database Migrations**
   ```bash
   # Execute the SQL script
   mysql -u root -p samaysetu_db < add_batches_and_timeslot_types.sql
   ```

5. **Start Backend Server**
   ```bash
   cd Backend
   mvn spring-boot:run
   ```

### Frontend Setup

1. **Install Dependencies**
   ```bash
   cd Frontend
   npm install
   ```

2. **Start Development Server**
   ```bash
   npm run dev
   ```

### Initial Setup

1. **Create Admin User**
   ```sql
   -- Run this SQL to create initial admin
   INSERT INTO users (name, email, password, role, is_active, is_email_verified) 
   VALUES ('System Admin', 'admin@mitaoe.ac.in', '$2a$10$hashedpassword', 'ADMIN', true, true);
   ```

2. **Access the Application**
   - Frontend: `http://localhost:5173`
   - Backend API: `http://localhost:8083`

## 📱 User Roles & Permissions

### 🔑 Admin
- Complete system access
- Manage academic structure
- Staff approval and management
- Timetable creation and modification
- System configuration

### 👨‍🏫 Teacher
- View personal timetable
- Update profile information
- Access assigned courses and divisions

### 👨‍🎓 Student
- View class timetables
- Access course information
- View division and batch details

## 🛠️ Technology Stack

### Backend
- **Framework**: Spring Boot 3.0+
- **Language**: Java 17
- **Database**: MySQL 8.0
- **Security**: Spring Security + JWT
- **ORM**: JPA/Hibernate
- **Build Tool**: Maven

### Frontend
- **Framework**: React 18
- **Language**: TypeScript
- **Styling**: Tailwind CSS
- **State Management**: React Hooks
- **HTTP Client**: Axios
- **Icons**: React Icons

## 📋 API Documentation

### Authentication Endpoints
```
POST /auth/login          # User login
POST /auth/register       # User registration (disabled)
GET  /auth/verify-email   # Email verification
POST /auth/forgot-password # Password reset request
POST /auth/reset-password  # Password reset
```

### Admin Endpoints
```
GET    /admin/api/academic-years     # Get all academic years
POST   /admin/api/academic-years     # Create academic year
GET    /admin/api/departments        # Get all departments
POST   /admin/api/departments/copy   # Copy departments between years
POST   /admin/upload-staff          # Bulk staff upload
POST   /admin/upload-courses        # Bulk course upload
```

## 🔧 Configuration

### Email Configuration
```properties
# SMTP settings for email verification
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=samaysetu.mitaoe@gmail.com
spring.mail.password=your-app-password
```

### JWT Configuration
```properties
# JWT token settings
jwt.secret=SamaySetu_MIT_AOE_2024_Secret_Key
jwt.expiration=86400000
```

## 📊 Database Schema

### Core Entities
- **Users** - Authentication and user management
- **Academic Years** - Academic year definitions
- **Departments** - Academic departments by year
- **Courses** - Subject/course definitions
- **Divisions** - Class divisions with teachers
- **Batches** - Student batch organization
- **Time Slots** - Scheduling time periods
- **Rooms** - Classroom management

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👥 Team

- **Project Lead**: MIT Academy of Engineering Development Team
- **Backend Developer**: Java Spring Boot Team
- **Frontend Developer**: React TypeScript Team
- **Database Designer**: MySQL Database Team

## 🎯 Future Enhancements

- [ ] Mobile application (React Native)
- [ ] Automated timetable generation
- [ ] Conflict detection and resolution
- [ ] Student attendance integration
- [ ] Parent portal access
- [ ] SMS notifications
- [ ] Advanced reporting and analytics
- [ ] Multi-language support

---

**Made with ❤️ for MIT Academy of Engineering and educational institutions worldwide**
