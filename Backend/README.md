# ⚙️ SamaySetu Backend

Robust Spring Boot backend for SamaySetu Timetable Management System with comprehensive API endpoints and security features.

## ✨ Latest Features (v3.0)

- ✅ **Academic Year Separation** - Complete isolation with department copying functionality
- ✅ **Advanced Staff Management** - Bulk operations, approval workflow, CSV import/export
- ✅ **Hierarchical Academic Structure** - Years → Departments → Divisions → Batches → Courses
- ✅ **Time Slot Management** - Multiple schedule types with overlap validation
- ✅ **JWT Authentication** - Secure token-based authentication with role-based access
- ✅ **Email Integration** - Verification, password reset, and notifications
- ✅ **CSV Operations** - Bulk data import/export with error handling
- ✅ **Comprehensive Validation** - Input validation with detailed error messages
- ✅ **RESTful API Design** - Clean, consistent API endpoints
- ✅ **Database Optimization** - Efficient queries with caching support

## 🚀 Quick Start

### Prerequisites
- **Java 17+** (OpenJDK or Oracle JDK)
- **Maven 3.6+** for dependency management
- **MySQL 8.0+** database server
- **SMTP Server** for email functionality (Gmail recommended)

### 1. Clone and Setup
```bash
git clone https://github.com/mitaoe/SamaySetu.git
cd SamaySetu/Backend
```

### 2. Database Configuration
```sql
-- Create database
CREATE DATABASE samaysetu_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Create user (optional)
CREATE USER 'samaysetu_user'@'localhost' IDENTIFIED BY 'samaysetu_password';
GRANT ALL PRIVILEGES ON samaysetu_db.* TO 'samaysetu_user'@'localhost';
FLUSH PRIVILEGES;
```

### 3. Application Configuration
Update `src/main/resources/application.properties`:
```properties
# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/samaysetu_db
spring.datasource.username=samaysetu_user
spring.datasource.password=samaysetu_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA/Hibernate Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
spring.jpa.properties.hibernate.format_sql=true

# JWT Configuration
jwt.secret=SamaySetu_MIT_AOE_2024_Secret_Key_Very_Long_And_Secure
jwt.expiration=86400000

# Email Configuration (Gmail SMTP)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=samaysetu.mitaoe@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# Application Configuration
server.port=8083
spring.application.name=SamaySetu-Backend

# File Upload Configuration
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# Logging Configuration
logging.level.com.College.timetable=INFO
logging.level.org.springframework.security=DEBUG
```

### 4. Run Database Migrations
```bash
# Execute the SQL migration script
mysql -u samaysetu_user -p samaysetu_db < ../add_batches_and_timeslot_types.sql
```

### 5. Build and Run
```bash
# Build the application
mvn clean compile

# Run the application
mvn spring-boot:run

# Or build JAR and run
mvn clean package
java -jar target/timetable-0.0.1-SNAPSHOT.jar
```

The backend will start on `http://localhost:8083`

## 📁 Project Structure

```
Backend/
├── src/main/java/com/College/timetable/
│   ├── Controller/          # REST API endpoints
│   │   ├── AuthController.java
│   │   ├── AdminController.java
│   │   ├── DepartmentController.java
│   │   ├── CourseController.java
│   │   ├── DivisionController.java
│   │   ├── BatchController.java
│   │   ├── TimeSlotController.java
│   │   └── TeacherController.java
│   ├── Service/             # Business logic layer
│   │   ├── AuthService.java
│   │   ├── DepartmentService.java
│   │   ├── CourseService.java
│   │   ├── EmailService.java
│   │   ├── TimeSlotService.java
│   │   └── UserService.java
│   ├── Repository/          # Data access layer
│   │   ├── Acadamic_repo.java
│   │   ├── Dep_repo.java
│   │   ├── Course_repo.java
│   │   ├── Division_repo.java
│   │   ├── Batch_repo.java
│   │   ├── TimeSlot_repo.java
│   │   └── Teacher_Repo.java
│   ├── Entity/              # JPA entity models
│   │   ├── AcademicYear.java
│   │   ├── DepartmentEntity.java
│   │   ├── CourseEntity.java
│   │   ├── Division.java
│   │   ├── Batch.java
│   │   ├── TimeSlot.java
│   │   ├── TeacherEntity.java
│   │   └── ClassRoom.java
│   ├── Security/            # Authentication & authorization
│   │   ├── JwtAuthenticationEntryPoint.java
│   │   ├── JwtAuthenticationFilter.java
│   │   ├── JwtTokenUtil.java
│   │   └── SecurityConfig.java
│   ├── IO/                  # Data transfer objects
│   │   ├── LoginRequest.java
│   │   ├── RegisterRequest.java
│   │   ├── ManualStaffRequest.java
│   │   └── AdminStaffUpdateRequest.java
│   ├── Exception/           # Custom exception handling
│   │   ├── GlobalExceptionHandler.java
│   │   └── CustomExceptions.java
│   └── TimetableApplication.java  # Main application class
├── src/main/resources/
│   ├── application.properties     # Configuration file
│   └── static/                   # Static resources
├── src/test/                     # Test classes
├── pom.xml                       # Maven dependencies
└── README.md                     # This file
```

## 🔐 Security Architecture

### JWT Authentication Flow:
1. **Login** → Validate credentials → Generate JWT token
2. **Token Validation** → Extract user info → Authorize requests
3. **Role-Based Access** → ADMIN, TEACHER, STUDENT roles
4. **Token Refresh** → Automatic token renewal

### Security Features:
- **Password Encryption** - BCrypt hashing
- **CORS Configuration** - Cross-origin request handling
- **Input Validation** - Bean validation with custom messages
- **SQL Injection Prevention** - JPA/Hibernate parameterized queries
- **XSS Protection** - Input sanitization

## 📊 Database Schema

### Core Entities:

#### Academic Structure:
```sql
academic_years (id, year_name, start_date, end_date, is_current)
departments (id, name, code, head_of_department, years, academic_year_id)
divisions (id, name, year, branch, total_students, time_slot_type, class_teacher, class_representative, department_id, academic_year_id)
batches (id, name, division_id)
courses (id, name, code, course_type, credits, hours_per_week, semester, year, department_id)
```

#### User Management:
```sql
teachers (id, name, employee_id, email, phone, password, role, specialization, weekly_hours_limit, is_active, is_email_verified, department_id)
```

#### Scheduling:
```sql
time_slots (id, slot_name, start_time, end_time, type, is_break, is_active)
class_rooms (id, name, room_number, capacity, room_type, has_projector, has_ac, equipment, department_id)
```

### Relationships:
- **Academic Years** → **Departments** (One-to-Many)
- **Departments** → **Divisions, Courses, Teachers, Rooms** (One-to-Many)
- **Divisions** → **Batches** (One-to-Many)
- **Teachers** → **Courses** (Many-to-Many)

## 🛠️ API Endpoints

### Authentication Endpoints:
```http
POST   /auth/login                    # User login
POST   /auth/register                 # User registration (disabled)
GET    /auth/verify-email             # Email verification
POST   /auth/forgot-password          # Password reset request
POST   /auth/reset-password           # Password reset
POST   /auth/change-first-password    # First-time password change
POST   /auth/                         # Generate password hash (utility)
```

### Admin Endpoints:
```http
# Academic Years
GET    /admin/api/academic-years      # Get all academic years
POST   /admin/api/academic-years      # Create academic year
PUT    /admin/api/academic-years/{id} # Update academic year
DELETE /admin/api/academic-years/{id} # Delete academic year
GET    /admin/api/academic-years/current # Get current academic year

# Departments
GET    /admin/api/departments         # Get all departments
GET    /admin/api/departments/academic-year/{id} # Get by academic year
POST   /admin/api/departments         # Create department
PUT    /admin/api/departments/{id}    # Update department
DELETE /admin/api/departments/{id}    # Delete department
POST   /admin/api/departments/copy    # Copy departments between years

# Courses
GET    /admin/api/courses             # Get all courses
POST   /admin/api/courses             # Create course
PUT    /admin/api/courses/{id}        # Update course
DELETE /admin/api/courses/{id}        # Delete course

# Divisions
GET    /admin/api/divisions           # Get all divisions
GET    /admin/api/divisions/academic-year/{id} # Get by academic year
POST   /admin/api/divisions           # Create division
PUT    /admin/api/divisions/{id}      # Update division
DELETE /admin/api/divisions/{id}      # Delete division

# Batches
GET    /admin/api/batches             # Get all batches
GET    /admin/api/batches/division/{id} # Get by division
POST   /admin/api/batches             # Create batch
PUT    /admin/api/batches/{id}        # Update batch
DELETE /admin/api/batches/{id}        # Delete batch

# Time Slots
GET    /admin/api/time-slots          # Get all time slots
GET    /admin/api/time-slots/type/{type} # Get by type
POST   /admin/api/time-slots          # Create time slot
PUT    /admin/api/time-slots/{id}     # Update time slot
DELETE /admin/api/time-slots/{id}     # Delete time slot

# Rooms
GET    /admin/api/rooms               # Get all rooms
POST   /admin/api/rooms               # Create room
PUT    /admin/api/rooms/{id}          # Update room
DELETE /admin/api/rooms/{id}          # Delete room
```

### Staff Management:
```http
POST   /admin/upload-staff            # Bulk staff upload (CSV)
GET    /admin/download-staff-template # Download staff CSV template
POST   /admin/create-staff            # Create staff manually
PUT    /admin/update-staff/{id}       # Update staff
POST   /admin/upload-courses          # Bulk course upload (CSV)
GET    /admin/download-courses-template # Download course CSV template
```

### Teacher Endpoints:
```http
GET    /api/teachers                  # Get all teachers
GET    /api/teachers/{id}             # Get teacher by ID
GET    /api/teachers/profile          # Get current user profile
PUT    /api/teachers/profile          # Update current user profile
GET    /api/teachers/pending-approvals # Get pending approvals (Admin)
POST   /api/teachers/{id}/approve     # Approve teacher (Admin)
POST   /api/teachers/{id}/reject      # Reject teacher (Admin)
```

### Staff Profile:
```http
GET    /api/staff/profile             # Get staff profile
PUT    /api/staff/profile             # Update staff profile (restricted)
POST   /api/staff/change-password     # Change password
```

## 📝 Request/Response Examples

### Create Department:
```json
POST /admin/api/departments
{
  "name": "Computer Engineering",
  "code": "COMP",
  "headOfDepartment": "Dr. John Smith",
  "years": "1,2,3,4",
  "academicYear": {
    "id": 1
  }
}
```

### Create Division:
```json
POST /admin/api/divisions
{
  "name": "A",
  "year": 2,
  "branch": "Computer Science",
  "totalStudents": 60,
  "timeSlotType": "TYPE_1",
  "classTeacher": "Prof. Jane Smith",
  "classRepresentative": "John Doe",
  "department": { "id": 1 },
  "academicYear": { "id": 1 }
}
```

### Copy Departments:
```json
POST /admin/api/departments/copy
{
  "sourceAcademicYearId": 1,
  "targetAcademicYearId": 2,
  "departmentIds": [1, 2, 3]
}
```

## 🔧 Configuration Options

### Email Configuration:
```properties
# Gmail SMTP (recommended)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password

# Outlook SMTP
spring.mail.host=smtp-mail.outlook.com
spring.mail.port=587

# Custom SMTP
spring.mail.host=your-smtp-server.com
spring.mail.port=587
```

### JWT Configuration:
```properties
# JWT Secret (use a strong, unique key)
jwt.secret=your-very-long-and-secure-secret-key-here
jwt.expiration=86400000  # 24 hours in milliseconds
```

### Database Configuration:
```properties
# MySQL (recommended)
spring.datasource.url=jdbc:mysql://localhost:3306/samaysetu_db
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

# PostgreSQL (alternative)
spring.datasource.url=jdbc:postgresql://localhost:5432/samaysetu_db
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

## 🧪 Testing

### Run Tests:
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=DepartmentServiceTest

# Run with coverage
mvn test jacoco:report
```

### Test Categories:
- **Unit Tests** - Service layer testing
- **Integration Tests** - Controller and repository testing
- **Security Tests** - Authentication and authorization testing

## 🚀 Deployment

### Development:
```bash
mvn spring-boot:run
```

### Production JAR:
```bash
mvn clean package -DskipTests
java -jar target/timetable-0.0.1-SNAPSHOT.jar
```

### Docker Deployment:
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/timetable-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Environment Variables:
```bash
export DB_URL=jdbc:mysql://localhost:3306/samaysetu_db
export DB_USERNAME=samaysetu_user
export DB_PASSWORD=samaysetu_password
export JWT_SECRET=your-secret-key
export MAIL_USERNAME=your-email@gmail.com
export MAIL_PASSWORD=your-app-password
```

## 📊 Performance Optimization

### Database Optimization:
- **Connection Pooling** - HikariCP configuration
- **Query Optimization** - Efficient JPA queries
- **Indexing** - Database indexes on frequently queried columns
- **Caching** - Spring Cache abstraction

### Application Optimization:
- **Lazy Loading** - JPA lazy loading for relationships
- **Pagination** - Large dataset pagination
- **Compression** - Response compression
- **Async Processing** - Email sending and bulk operations

## 🐛 Troubleshooting

### Common Issues:

**Database Connection:**
```bash
# Check MySQL service
sudo systemctl status mysql

# Test connection
mysql -u samaysetu_user -p -h localhost samaysetu_db
```

**Email Configuration:**
```bash
# Gmail App Password required (not regular password)
# Enable 2FA and generate app password
```

**JWT Token Issues:**
```bash
# Check token expiration
# Verify secret key consistency
# Clear browser localStorage
```

**Port Already in Use:**
```properties
# Change port in application.properties
server.port=8084
```

**Memory Issues:**
```bash
# Increase JVM memory
java -Xmx2g -jar target/timetable-0.0.1-SNAPSHOT.jar
```

## 📈 Monitoring & Logging

### Application Monitoring:
```properties
# Actuator endpoints
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=always
```

### Logging Configuration:
```properties
# Log levels
logging.level.com.College.timetable=INFO
logging.level.org.springframework.security=DEBUG
logging.level.org.hibernate.SQL=DEBUG

# Log file
logging.file.name=logs/samaysetu.log
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss} - %msg%n
```

## 🔮 Future Enhancements

- [ ] **Automated Timetable Generation** - AI-based scheduling
- [ ] **Conflict Detection Engine** - Advanced overlap prevention
- [ ] **WebSocket Integration** - Real-time updates
- [ ] **Advanced Analytics** - Usage statistics and reports
- [ ] **Multi-tenant Support** - Multiple institutions
- [ ] **Mobile API Optimization** - Lightweight endpoints
- [ ] **Microservices Architecture** - Service decomposition
- [ ] **Event Sourcing** - Audit trail and history

## 📞 Support & Documentation

- **API Documentation**: `../SamaySetu_Postman_Collection.json`
- **Frontend Documentation**: `../Frontend/README.md`
- **Database Schema**: `../add_batches_and_timeslot_types.sql`
- **Project Overview**: `../README.md`

## 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Open Pull Request

### Development Guidelines:
- Follow Spring Boot best practices
- Write comprehensive tests
- Use proper exception handling
- Document API changes
- Follow RESTful conventions

---

**Built with ❤️ for MIT Academy of Engineering, Alandi, Pune**

© 2026 MIT Academy of Engineering - SamaySetu Development Team