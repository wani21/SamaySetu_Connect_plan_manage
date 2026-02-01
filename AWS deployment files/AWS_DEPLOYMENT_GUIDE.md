# AWS Deployment Guide for SamaySetu

This guide explains how to deploy SamaySetu to AWS using your $200 free tier credits.

## 🎯 Recommended AWS Architecture

### Option 1: AWS Elastic Beanstalk + Amplify (Simplest)
- **Backend**: AWS Elastic Beanstalk (Java application)
- **Frontend**: AWS Amplify (React application)
- **Database**: Supabase (already configured)
- **Cost**: ~$10-20/month (within free tier)

### Option 2: AWS App Runner + Amplify (Modern)
- **Backend**: AWS App Runner (Container-based)
- **Frontend**: AWS Amplify
- **Database**: Supabase
- **Cost**: ~$15-25/month (within free tier)

### Option 3: AWS ECS Fargate + S3/CloudFront (Advanced)
- **Backend**: ECS Fargate (Docker containers)
- **Frontend**: S3 + CloudFront
- **Database**: Supabase
- **Cost**: ~$20-30/month (within free tier)

## 📋 Prerequisites

1. AWS Account with $200 credits
2. GitHub repository with your code
3. Supabase database (already configured)
4. AWS CLI installed (optional but recommended)

---

## 🚀 OPTION 1: Elastic Beanstalk + Amplify (RECOMMENDED)

This is the **simplest** option and perfect for your use case.

### Part A: Deploy Backend to Elastic Beanstalk

#### Step 1: Prepare Backend for Deployment

1. **Update `application.properties`** to use production profile:
   ```properties
   spring.profiles.active=prod
   ```

2. **Build the JAR file**:
   ```bash
   cd Backend
   ./mvnw clean package -DskipTests
   ```
   
   This creates: `Backend/target/samaysetu-0.0.1-SNAPSHOT.jar`

#### Step 2: Create Elastic Beanstalk Application

1. Go to AWS Console → **Elastic Beanstalk**
2. Click **Create Application**
3. Configure:
   - **Application name**: `samaysetu-backend`
   - **Platform**: Java
   - **Platform branch**: Corretto 17 (or Java 17)
   - **Application code**: Upload your JAR file from `Backend/target/`

4. Click **Configure more options**
5. Under **Software**, add environment variables:
   ```
   SPRING_DATASOURCE_URL=jdbc:postgresql://db.tehdpecquwvgwpombtbl.supabase.co:5432/postgres
   SPRING_DATASOURCE_USERNAME=postgres
   SPRING_DATASOURCE_PASSWORD=samaysetumitaoe
   SERVER_PORT=5000
   ```

6. Under **Capacity**, select:
   - **Environment type**: Single instance (free tier)
   - **Instance type**: t2.micro or t3.micro (free tier eligible)

7. Click **Create application**

8. Wait 5-10 minutes for deployment

9. **Note your backend URL**: `http://samaysetu-backend.us-east-1.elasticbeanstalk.com`

#### Step 3: Configure CORS in Backend

Update `SecurityConfig.java` to allow your frontend domain:
```java
.cors(cors -> cors.configurationSource(request -> {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(Arrays.asList(
        "http://localhost:5173",
        "https://your-amplify-domain.amplifyapp.com"  // Add after frontend deployment
    ));
    // ... rest of CORS config
}))
```

### Part B: Deploy Frontend to AWS Amplify

#### Step 1: Push Code to GitHub

1. Create a GitHub repository (if not already done)
2. Push your code:
   ```bash
   git add .
   git commit -m "Prepare for AWS deployment"
   git push origin main
   ```

#### Step 2: Deploy to AWS Amplify

1. Go to AWS Console → **AWS Amplify**
2. Click **New app** → **Host web app**
3. Select **GitHub** and authorize
4. Select your repository and branch (`main`)
5. Configure build settings:
   - **App name**: `samaysetu-frontend`
   - **Build and test settings**: Auto-detected (React/Vite)

6. Add environment variable:
   - Key: `VITE_API_URL`
   - Value: `http://samaysetu-backend.us-east-1.elasticbeanstalk.com` (your backend URL)

7. Click **Save and deploy**

8. Wait 5-10 minutes for deployment

9. **Your app is live!** URL: `https://main.d1234567890.amplifyapp.com`

#### Step 3: Update Backend CORS

Go back to Elastic Beanstalk and update the CORS configuration with your Amplify URL.

---

## 🚀 OPTION 2: AWS App Runner (Container-based)

If you prefer using Docker containers:

### Part A: Deploy Backend to App Runner

#### Step 1: Create Dockerfile for Backend

Create `Backend/Dockerfile`:
```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### Step 2: Push to Amazon ECR

1. Create ECR repository:
   ```bash
   aws ecr create-repository --repository-name samaysetu-backend
   ```

2. Build and push image:
   ```bash
   aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin YOUR_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com
   
   docker build -t samaysetu-backend ./Backend
   docker tag samaysetu-backend:latest YOUR_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com/samaysetu-backend:latest
   docker push YOUR_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com/samaysetu-backend:latest
   ```

#### Step 3: Create App Runner Service

1. Go to AWS Console → **App Runner**
2. Click **Create service**
3. Configure:
   - **Source**: Container registry → Amazon ECR
   - **Image**: Select your ECR image
   - **Port**: 8083
   - **Environment variables**: Add database credentials
4. Click **Create & deploy**

### Part B: Deploy Frontend to Amplify

Same as Option 1, Part B.

---

## 🚀 OPTION 3: Manual Deployment Steps

### Quick Deployment Checklist

#### Before Pushing to GitHub:

1. ✅ Change `application.properties`:
   ```properties
   spring.profiles.active=prod
   ```

2. ✅ Build backend JAR:
   ```bash
   cd Backend
   ./mvnw clean package -DskipTests
   ```

3. ✅ Commit and push to GitHub:
   ```bash
   git add .
   git commit -m "Deploy to AWS"
   git push origin main
   ```

#### In AWS Console:

1. ✅ Deploy backend to Elastic Beanstalk (upload JAR)
2. ✅ Set environment variables in Elastic Beanstalk
3. ✅ Note backend URL
4. ✅ Deploy frontend to Amplify (connect GitHub)
5. ✅ Set `VITE_API_URL` in Amplify environment variables
6. ✅ Update CORS in backend with Amplify URL

---

## 🔄 Auto-Update Workflow

Once deployed, updates are automatic:

### For Backend (Elastic Beanstalk):
1. Make code changes
2. Change profile to `prod` in `application.properties`
3. Build JAR: `./mvnw clean package -DskipTests`
4. Upload new JAR to Elastic Beanstalk
5. Elastic Beanstalk automatically deploys

### For Frontend (Amplify):
1. Make code changes
2. Push to GitHub: `git push origin main`
3. Amplify automatically detects changes and rebuilds
4. New version is live in 5-10 minutes

---

## 💰 Cost Estimation (Free Tier)

| Service | Free Tier | Expected Cost |
|---------|-----------|---------------|
| Elastic Beanstalk | 750 hours/month (t2.micro) | $0 |
| AWS Amplify | 1000 build minutes, 15GB storage | $0 |
| Data Transfer | 100GB/month | $0 |
| **Total** | | **$0/month** (within free tier) |

After free tier expires (~12 months):
- Elastic Beanstalk: ~$15/month
- Amplify: ~$5/month
- **Total**: ~$20/month

---

## 🔧 Environment Variables Reference

### Backend (Elastic Beanstalk)
```
SPRING_DATASOURCE_URL=jdbc:postgresql://db.tehdpecquwvgwpombtbl.supabase.co:5432/postgres
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=samaysetumitaoe
SERVER_PORT=5000
APP_BASE_URL=http://samaysetu-backend.elasticbeanstalk.com
```

### Frontend (Amplify)
```
VITE_API_URL=http://samaysetu-backend.elasticbeanstalk.com
```

---

## 🐛 Troubleshooting

### Backend not connecting to Supabase
- Check environment variables in Elastic Beanstalk
- Verify Supabase allows connections from AWS IPs
- Check security groups allow outbound traffic on port 5432

### Frontend can't reach backend
- Verify CORS configuration in backend
- Check `VITE_API_URL` environment variable in Amplify
- Ensure backend URL is correct (no trailing slash)

### Build fails
- Check Java version (must be 17)
- Verify all dependencies in `pom.xml`
- Check build logs in Elastic Beanstalk or Amplify

---

## 📞 Support

If you encounter issues:
1. Check AWS CloudWatch logs
2. Verify environment variables
3. Test backend health endpoint: `http://your-backend-url/actuator/health`
4. Check Supabase connection from AWS

---

## ✅ Success Checklist

- [ ] Backend deployed to Elastic Beanstalk
- [ ] Backend environment variables configured
- [ ] Backend health check passing
- [ ] Frontend deployed to Amplify
- [ ] Frontend environment variable set
- [ ] CORS configured correctly
- [ ] Login works end-to-end
- [ ] Professor can access via Amplify URL
- [ ] Auto-deploy working from GitHub

---

**Your professor will access the app at**: `https://main.d1234567890.amplifyapp.com`

**No Docker, no manual setup, just a URL!** 🎉
