# 🔄 How to Switch Between Development and Production

## 📌 Overview

Your project now uses **Spring Profiles** to manage different environments:
- **Development (dev)**: For local development with localhost URLs
- **Production (prod)**: For AWS deployment with production URLs

## 🎯 The Simple Way

### To Deploy to AWS (Production):

**Step 1**: Open `Backend/src/main/resources/application.properties`

**Step 2**: Change this line:
```properties
spring.profiles.active=dev
```

To:
```properties
spring.profiles.active=prod
```

**Step 3**: Build and deploy:
```bash
cd Backend
mvnw.cmd clean package -DskipTests
```

**That's it!** No need to change any localhost URLs manually.

---

### To Go Back to Development:

**Step 1**: Open `Backend/src/main/resources/application.properties`

**Step 2**: Change this line:
```properties
spring.profiles.active=prod
```

To:
```properties
spring.profiles.active=dev
```

**Step 3**: Run locally:
```bash
cd Backend
mvnw.cmd spring-boot:run
```

---

## 📁 Configuration Files Explained

### 1. `application.properties` (Main Config)
```properties
# This is the ONLY file you need to edit
spring.profiles.active=dev  # Change to 'prod' for AWS deployment
spring.application.name=samaysetu
```

This file just sets which profile to use. That's it!

### 2. `application-dev.properties` (Development)
```properties
# Used when: spring.profiles.active=dev
spring.datasource.url=jdbc:postgresql://db.tehdpecquwvgwpombtbl.supabase.co:5432/postgres
app.base-url=http://localhost:8083
logging.level.org.hibernate.SQL=DEBUG  # Verbose logging for debugging
```

This is automatically loaded when profile is `dev`.

### 3. `application-prod.properties` (Production)
```properties
# Used when: spring.profiles.active=prod
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://...}
app.base-url=${APP_BASE_URL:https://your-backend-url.amazonaws.com}
logging.level.org.hibernate.SQL=WARN  # Less verbose for production
```

This is automatically loaded when profile is `prod`.

---

## 🎨 Frontend Configuration

### Development
Frontend automatically uses `http://localhost:8083` when running locally:
```bash
cd Frontend
npm run dev
```

### Production
Frontend uses the `VITE_API_URL` environment variable set in AWS Amplify.

**No code changes needed!** Just set the environment variable in AWS Amplify:
- Key: `VITE_API_URL`
- Value: `http://your-backend-url.elasticbeanstalk.com`

---

## 🔧 Environment Variables (Production Only)

When deploying to AWS, these environment variables override the defaults:

### Backend (Elastic Beanstalk)
```
SPRING_DATASOURCE_URL=jdbc:postgresql://db.tehdpecquwvgwpombtbl.supabase.co:5432/postgres
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=samaysetumitaoe
SERVER_PORT=5000
SPRING_PROFILES_ACTIVE=prod
```

### Frontend (Amplify)
```
VITE_API_URL=http://samaysetu-backend.elasticbeanstalk.com
```

---

## ✅ Quick Reference

| Task | Command | Profile |
|------|---------|---------|
| Run locally | `mvnw.cmd spring-boot:run` | `dev` |
| Build for AWS | `mvnw.cmd clean package -DskipTests` | `prod` |
| Test locally | `java -jar target/*.jar` | `dev` or `prod` |

---

## 🎯 Workflow Example

### Scenario: You want to deploy to AWS

1. **Edit one file**: `application.properties`
   ```properties
   spring.profiles.active=prod  # Change from 'dev' to 'prod'
   ```

2. **Build**:
   ```bash
   cd Backend
   mvnw.cmd clean package -DskipTests
   ```

3. **Deploy**: Upload JAR to Elastic Beanstalk

4. **Done!** ✅

### Scenario: You want to continue local development

1. **Edit one file**: `application.properties`
   ```properties
   spring.profiles.active=dev  # Change from 'prod' to 'dev'
   ```

2. **Run**:
   ```bash
   cd Backend
   mvnw.cmd spring-boot:run
   ```

3. **Done!** ✅

---

## 🚫 What You DON'T Need to Do

❌ Change localhost URLs in code  
❌ Edit multiple configuration files  
❌ Modify frontend API URLs  
❌ Update database connection strings manually  
❌ Change port numbers  

✅ Just change **one line** in `application.properties`!

---

## 🐛 Troubleshooting

### "Wrong profile is active"
Check `application.properties` line 4:
```properties
spring.profiles.active=dev  # or 'prod'
```

### "Can't connect to database"
- **Dev**: Check Supabase is accessible
- **Prod**: Check environment variables in AWS Elastic Beanstalk

### "Frontend can't reach backend"
- **Dev**: Backend should be running on `http://localhost:8083`
- **Prod**: Check `VITE_API_URL` in AWS Amplify environment variables

---

## 📝 Summary

**One file to rule them all**: `application.properties`

**One line to change**: `spring.profiles.active=dev` or `prod`

**No manual URL changes needed!** 🎉

---

**That's the power of Spring Profiles!** 🚀
