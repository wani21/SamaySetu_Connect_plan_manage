# 🚀 AWS Deployment Checklist

Use this checklist before deploying to AWS.

## 📋 Pre-Deployment Steps

### 1. Switch to Production Profile
- [ ] Open `Backend/src/main/resources/application.properties`
- [ ] Change line 4 to: `spring.profiles.active=prod`
- [ ] Save the file

### 2. Build Backend JAR
```bash
cd Backend
mvnw.cmd clean package -DskipTests
```
- [ ] Build successful
- [ ] JAR file created at: `Backend/target/samaysetu-0.0.1-SNAPSHOT.jar`

### 3. Test Locally (Optional)
```bash
java -jar Backend/target/samaysetu-0.0.1-SNAPSHOT.jar
```
- [ ] Application starts without errors
- [ ] Can connect to Supabase database

### 4. Commit and Push to GitHub
```bash
git add .
git commit -m "Deploy to AWS - Production ready"
git push origin main
```
- [ ] Code pushed to GitHub successfully

---

## 🔧 AWS Backend Deployment (Elastic Beanstalk)

### 1. Create Application
- [ ] Go to AWS Console → Elastic Beanstalk
- [ ] Click "Create Application"
- [ ] Application name: `samaysetu-backend`
- [ ] Platform: Java
- [ ] Platform branch: Corretto 17
- [ ] Upload JAR: `Backend/target/samaysetu-0.0.1-SNAPSHOT.jar`

### 2. Configure Environment Variables
Click "Configure more options" → Software → Edit

Add these environment variables:
- [ ] `SPRING_DATASOURCE_URL` = `jdbc:postgresql://db.tehdpecquwvgwpombtbl.supabase.co:5432/postgres`
- [ ] `SPRING_DATASOURCE_USERNAME` = `postgres`
- [ ] `SPRING_DATASOURCE_PASSWORD` = `samaysetumitaoe`
- [ ] `SERVER_PORT` = `5000`
- [ ] `SPRING_PROFILES_ACTIVE` = `prod`

### 3. Configure Capacity
Click "Capacity" → Edit
- [ ] Environment type: Single instance
- [ ] Instance type: t2.micro (free tier)

### 4. Create and Deploy
- [ ] Click "Create application"
- [ ] Wait 5-10 minutes for deployment
- [ ] Health status shows "Ok" (green)
- [ ] Copy backend URL: `http://samaysetu-backend.elasticbeanstalk.com`

### 5. Test Backend
- [ ] Open: `http://your-backend-url/actuator/health`
- [ ] Should return: `{"status":"UP"}`

---

## 🎨 AWS Frontend Deployment (Amplify)

### 1. Connect GitHub
- [ ] Go to AWS Console → AWS Amplify
- [ ] Click "New app" → "Host web app"
- [ ] Select "GitHub" and authorize
- [ ] Select repository and branch (`main`)

### 2. Configure Build
- [ ] App name: `samaysetu-frontend`
- [ ] Build settings: Auto-detected (should detect Vite/React)
- [ ] Root directory: `Frontend`

### 3. Add Environment Variable
- [ ] Key: `VITE_API_URL`
- [ ] Value: `http://samaysetu-backend.elasticbeanstalk.com` (your backend URL)

### 4. Deploy
- [ ] Click "Save and deploy"
- [ ] Wait 5-10 minutes for build and deployment
- [ ] Build status shows "Deployed" (green)
- [ ] Copy frontend URL: `https://main.d1234567890.amplifyapp.com`

### 5. Test Frontend
- [ ] Open frontend URL in browser
- [ ] Login page loads correctly
- [ ] Try logging in with: `admin@mitaoe.ac.in` / `admin123`
- [ ] Dashboard loads successfully

---

## ✅ Post-Deployment Verification

### Backend Health Check
- [ ] Backend URL accessible
- [ ] Health endpoint returns OK
- [ ] Can connect to Supabase database
- [ ] Logs show no errors

### Frontend Functionality
- [ ] Frontend loads without errors
- [ ] Can reach backend API
- [ ] Login works
- [ ] Dashboard displays data
- [ ] All pages accessible

### Integration Test
- [ ] Login as admin
- [ ] View departments
- [ ] View teachers
- [ ] View academic years
- [ ] All CRUD operations work

---

## 📝 Share with Professor

Once everything is working:

**Frontend URL**: `https://main.d1234567890.amplifyapp.com`

**Login Credentials**:
- Email: `admin@mitaoe.ac.in`
- Password: `admin123`

**Features to Demo**:
1. Admin Dashboard
2. Department Management
3. Teacher Management
4. Academic Year Management
5. Course Management
6. Room Management
7. Time Slot Management

---

## 🔄 Future Updates

### To Update Backend:
1. Make code changes
2. Change `spring.profiles.active=prod`
3. Build JAR: `mvnw.cmd clean package -DskipTests`
4. Go to Elastic Beanstalk → Upload new version
5. Wait 2-3 minutes for deployment

### To Update Frontend:
1. Make code changes
2. Push to GitHub: `git push origin main`
3. Amplify automatically rebuilds (5-10 minutes)
4. Changes are live!

---

## 🐛 Troubleshooting

### Backend Issues
- [ ] Check environment variables in Elastic Beanstalk
- [ ] View logs: Elastic Beanstalk → Logs → Request Logs
- [ ] Check security groups allow outbound traffic
- [ ] Verify Supabase connection

### Frontend Issues
- [ ] Check `VITE_API_URL` in Amplify environment variables
- [ ] View build logs in Amplify
- [ ] Check browser console for errors
- [ ] Verify backend URL is correct (no trailing slash)

### Connection Issues
- [ ] CORS configured correctly (already done)
- [ ] Backend health check passing
- [ ] Frontend can reach backend
- [ ] Database connection working

---

## 💰 Cost Tracking

Monitor your AWS costs:
- [ ] Set up billing alerts in AWS Console
- [ ] Check free tier usage dashboard
- [ ] Expected cost: $0/month (within free tier)

---

## ✨ Success!

- [ ] Backend deployed and healthy
- [ ] Frontend deployed and accessible
- [ ] Login works end-to-end
- [ ] Professor can access the application
- [ ] Auto-deploy configured for future updates

**Congratulations! Your project is live on AWS!** 🎉
