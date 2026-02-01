# Quick AWS Deployment Steps

## 🎯 Goal
Deploy SamaySetu to AWS so your professor can access it via a live URL.

## ⚡ Quick Steps (30 minutes)

### Step 1: Prepare for Deployment (5 minutes)

1. **Change to production profile**:
   - Open `Backend/src/main/resources/application.properties`
   - Change line 4 to: `spring.profiles.active=prod`

2. **Build backend JAR**:
   ```bash
   cd Backend
   mvnw.cmd clean package -DskipTests
   ```
   
   ✅ JAR file created at: `Backend/target/samaysetu-0.0.1-SNAPSHOT.jar`

3. **Push to GitHub**:
   ```bash
   git add .
   git commit -m "Deploy to AWS"
   git push origin main
   ```

---

### Step 2: Deploy Backend (10 minutes)

1. **Go to AWS Console** → Search "Elastic Beanstalk"

2. **Click "Create Application"**

3. **Fill in details**:
   - Application name: `samaysetu-backend`
   - Platform: `Java`
   - Platform branch: `Corretto 17`
   - Application code: Click "Upload" → Select `Backend/target/samaysetu-0.0.1-SNAPSHOT.jar`

4. **Click "Configure more options"**

5. **Under "Software"**, click "Edit" and add environment variables:
   ```
   SPRING_DATASOURCE_URL = jdbc:postgresql://db.tehdpecquwvgwpombtbl.supabase.co:5432/postgres
   SPRING_DATASOURCE_USERNAME = postgres
   SPRING_DATASOURCE_PASSWORD = samaysetumitaoe
   SERVER_PORT = 5000
   ```

6. **Under "Capacity"**, click "Edit":
   - Environment type: `Single instance`
   - Instance type: `t2.micro` (free tier)

7. **Click "Create application"**

8. **Wait 5-10 minutes** ⏳

9. **Copy your backend URL**: 
   Example: `http://samaysetu-backend.us-east-1.elasticbeanstalk.com`

---

### Step 3: Deploy Frontend (10 minutes)

1. **Go to AWS Console** → Search "Amplify"

2. **Click "New app" → "Host web app"**

3. **Select "GitHub"** and authorize

4. **Select your repository** and branch (`main`)

5. **Configure build**:
   - App name: `samaysetu-frontend`
   - Build settings: Auto-detected ✅

6. **Add environment variable**:
   - Key: `VITE_API_URL`
   - Value: `http://samaysetu-backend.us-east-1.elasticbeanstalk.com` (your backend URL from Step 2)

7. **Click "Save and deploy"**

8. **Wait 5-10 minutes** ⏳

9. **Your app is live!** 🎉
   URL: `https://main.d1234567890.amplifyapp.com`

---

### Step 4: Update CORS (5 minutes)

1. **Go back to Elastic Beanstalk**

2. **Click your application** → "Configuration" → "Software" → "Edit"

3. **Add environment variable**:
   ```
   ALLOWED_ORIGINS = https://main.d1234567890.amplifyapp.com
   ```
   (Replace with your actual Amplify URL)

4. **Click "Apply"**

5. **Wait 2-3 minutes for restart**

---

## ✅ Done!

**Share this URL with your professor**: `https://main.d1234567890.amplifyapp.com`

**Login credentials**:
- Email: `admin@mitaoe.ac.in`
- Password: `admin123`

---

## 🔄 How to Update Later

### Update Backend:
1. Change `spring.profiles.active=prod` in `application.properties`
2. Build JAR: `mvnw.cmd clean package -DskipTests`
3. Go to Elastic Beanstalk → Upload new JAR
4. Done! ✅

### Update Frontend:
1. Make your changes
2. Push to GitHub: `git push origin main`
3. Amplify automatically rebuilds and deploys
4. Done! ✅

---

## 💰 Cost

**$0/month** (within free tier for 12 months)

After free tier:
- ~$15/month for backend
- ~$5/month for frontend
- **Total: ~$20/month**

---

## 🐛 Troubleshooting

### Backend health check failing?
- Check environment variables in Elastic Beanstalk
- View logs: Elastic Beanstalk → Logs → Request Logs

### Frontend can't connect to backend?
- Verify `VITE_API_URL` in Amplify environment variables
- Check CORS configuration in backend

### Login not working?
- Check Supabase database is accessible
- Verify admin user exists in database
- Check browser console for errors

---

## 📞 Need Help?

Check AWS CloudWatch logs:
- Backend logs: Elastic Beanstalk → Logs
- Frontend logs: Amplify → Build logs

---

**That's it! Your project is now live on AWS!** 🚀
