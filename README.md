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
- **Supabase Account** (PostgreSQL database)
- **Maven 3.6+**

### Quick Start (Local Development)

1. **Clone the repository**
   ```bash
   git clone https://github.com/mitaoe/SamaySetu.git
   cd SamaySetu
   ```

2. **Configure Database**
   - Create a Supabase project at https://supabase.com
   - Run the migration script: `Scripts/supabase_complete_migration.sql`
   - Update `Backend/src/main/resources/application-dev.properties` with your credentials

3. **Start Backend Server**
   ```bash
   cd Backend
   mvnw.cmd spring-boot:run
   ```

4. **Start Frontend**
   ```bash
   cd Frontend
   npm install
   npm run dev
   ```

5. **Access the Application**
   - Frontend: `http://localhost:5173`
   - Backend API: `http://localhost:8083`
   - Login: `admin@mitaoe.ac.in` / `admin123`

### 🌐 AWS Deployment (Production) - FULLY AUTOMATED! 🤖

**Complete CI/CD Pipeline**: Just `git push` and both backend and frontend deploy automatically!

**📘 MAIN GUIDE**: [`COMPLETE_AWS_DEPLOYMENT_WITH_GITHUB_ACTIONS.md`](COMPLETE_AWS_DEPLOYMENT_WITH_GITHUB_ACTIONS.md)

**Setup (One-time, 75 minutes)**:
1. Create AWS IAM user for GitHub Actions
2. Deploy backend to Elastic Beanstalk (manual first time)
3. Deploy frontend to AWS Amplify (auto-deploy from GitHub)
4. Set up GitHub Actions workflow
5. **Future updates**: Just `git push origin main` → Auto-deploys! ✅

**Quick References**:
- ⚡ [`QUICK_START_GITHUB_ACTIONS.md`](QUICK_START_GITHUB_ACTIONS.md) - 75-minute quick start
- 🏗️ [`DEPLOYMENT_ARCHITECTURE.md`](DEPLOYMENT_ARCHITECTURE.md) - Architecture diagrams
- 📋 [`DEPLOYMENT_CHECKLIST.md`](DEPLOYMENT_CHECKLIST.md) - Deployment checklist
- 🔄 [`SWITCH_TO_PRODUCTION.md`](SWITCH_TO_PRODUCTION.md) - Spring profiles guide

**Features**:
- ✅ Backend auto-deploy via GitHub Actions
- ✅ Frontend auto-deploy via AWS Amplify
- ✅ Complete CI/CD pipeline
- ✅ Cost: $0/month (AWS free tier)
- ✅ [`AUTO_DEPLOY_SUMMARY.md`](AUTO_DEPLOY_SUMMARY.md) - Automation summary
- 📋 [`DEPLOYMENT_CHECKLIST.md`](DEPLOYMENT_CHECKLIST.md) - Deployment checklist

**Benefits**:
- ✅ No manual JAR uploads
- ✅ No AWS Console uploads
- ✅ Both frontend and backend auto-deploy
- ✅ Professional CI/CD pipeline
- ✅ Just `git push` to deploy!

**Cost**: $0/month (AWS free tier for 12 months)

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
- **Database**: Supabase (PostgreSQL)
- **Security**: Spring Security + JWT
- **ORM**: JPA/Hibernate
- **Build Tool**: Maven
- **Deployment**: AWS Elastic Beanstalk

### Frontend
- **Framework**: React 18
- **Language**: TypeScript
- **Styling**: Tailwind CSS
- **State Management**: Zustand
- **HTTP Client**: Axios
- **Icons**: React Icons
- **Deployment**: AWS Amplify

### Infrastructure
- **Database**: Supabase (PostgreSQL with RLS)
- **Backend Hosting**: AWS Elastic Beanstalk
- **Frontend Hosting**: AWS Amplify
- **CI/CD**: GitHub → Amplify (auto-deploy)

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

### Spring Profiles
The application uses Spring Profiles for environment management:

**Development** (`application-dev.properties`):
- Local development with localhost URLs
- Verbose logging for debugging
- Direct database connection

**Production** (`application-prod.properties`):
- AWS deployment with environment variables
- Optimized logging
- Production security settings

**Switch profiles** by changing one line in `application.properties`:
```properties
spring.profiles.active=dev  # or 'prod' for AWS
```

### Email Configuration
```properties
# SMTP settings for email verification
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=202301040228@mitaoe.ac.in
spring.mail.password=your-app-password
```

### JWT Configuration
```properties
# JWT token settings (configured in profile files)
jwt.secret.key=your-secret-key
```

## 📊 Database Schema

### Core Entities
- **users** (formerly teachers) - Authentication and user management
- **academic_years** - Academic year definitions
- **departments** - Academic departments by year
- **courses** - Subject/course definitions
- **divisions** - Class divisions with teachers
- **batches** - Student batch organization
- **time_slots** - Scheduling time periods
- **classrooms** (formerly rooms) - Classroom management with building/wing
- **user_availability** - Teacher availability tracking
- **timetable_entries** - Scheduled classes

### Database Migration
- Migrated from MySQL to Supabase (PostgreSQL)
- Complete migration script: `Scripts/supabase_complete_migration.sql`
- Row Level Security (RLS) policies implemented
- All data preserved during migration

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👥 Team

- **Project Lead**: MIT Academy of Engineering Development Team
- **Backend Developer**: Java Spring Boot Team
- **Frontend Developer**: React TypeScript Team
- **Database Designer**: MySQL Database Team

## 🎯 Future Enhancements

- [ ] Mobile application (React Native)
- [ ] Automated timetable generation with AI
- [ ] Conflict detection and resolution
- [ ] Student attendance integration
- [ ] Parent portal access
- [ ] SMS notifications
- [ ] Advanced reporting and analytics
- [ ] Multi-language support
- [ ] Real-time collaboration features

## 📚 Documentation

### Deployment Guides
- [`START_HERE_AWS_DEPLOYMENT.md`](START_HERE_AWS_DEPLOYMENT.md) - AWS deployment overview
- [`QUICK_AWS_DEPLOY.md`](QUICK_AWS_DEPLOY.md) - Quick deployment guide
- [`DEPLOYMENT_CHECKLIST.md`](DEPLOYMENT_CHECKLIST.md) - Deployment checklist
- [`DEPLOY_QUICK_REFERENCE.md`](DEPLOY_QUICK_REFERENCE.md) - One-page reference

### Configuration Guides
- [`SWITCH_TO_PRODUCTION.md`](SWITCH_TO_PRODUCTION.md) - Spring profiles guide
- [`AWS_SERVICES_COMPARISON.md`](AWS_SERVICES_COMPARISON.md) - AWS services comparison
- [`AWS_DEPLOYMENT_GUIDE.md`](AWS_DEPLOYMENT_GUIDE.md) - Detailed AWS guide

### Database
- [`Scripts/supabase_complete_migration.sql`](Scripts/supabase_complete_migration.sql) - Database migration
- [`database.txt`](database.txt) - Database credentials

### API Documentation
- [`SamaySetu_Postman_Collection.json`](SamaySetu_Postman_Collection.json) - Postman collection
- [`SamaySetu_Postman_Environment.json`](SamaySetu_Postman_Environment.json) - Postman environment

## 🚀 Live Demo

**Production URL**: Available after AWS deployment  
**Login**: `admin@mitaoe.ac.in` / `admin123`

## 💰 Cost

- **Development**: Free (Supabase free tier)
- **Production**: $0/month (AWS free tier for 12 months)
- **After Free Tier**: ~$13/month (AWS Elastic Beanstalk + Amplify)

---

**Made with ❤️ for MIT Academy of Engineering and educational institutions worldwide**
