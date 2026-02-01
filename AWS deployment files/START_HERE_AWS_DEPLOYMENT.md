# 🚀 START HERE: AWS Deployment for SamaySetu

## 📌 What Changed?

Your project now uses **Spring Profiles** for easy deployment to AWS!

### Before:
- ❌ Had to manually change localhost URLs
- ❌ Edit multiple files
- ❌ Docker issues on mobile hotspot
- ❌ Complex setup for professor

### Now:
- ✅ Just change **one line** to switch to production
- ✅ No manual URL changes needed
- ✅ Professor gets a simple URL
- ✅ Auto-updates from GitHub (frontend)
- ✅ Works on AWS free tier ($200 credits)

---

## 🎯 Quick Deploy (30 Minutes)

### Step 1: Switch to Production (1 minute)

Open `Backend/src/main/resources/application.properties` and change:

```properties
spring.profiles.active=dev
```

To:

```properties
spring.profiles.active=prod
```

**That's it!** No other changes needed.

---

### Step 2: Build Backend (2 minutes)

```bash
cd Backend
mvnw.cmd clean package -DskipTests
```

✅ Creates: `Backend/target/samaysetu-0.0.1-SNAPSHOT.jar`

---

### Step 3: Deploy Backend to AWS (10 minutes)

1. Go to **AWS Console** → **Elastic Beanstalk**
2. Click **"Create Application"**
3. Fill in:
   - Name: `samaysetu-backend`
   - Platform: **Java**
   - Platform branch: **Corretto 17**
   - Upload: `Backend/target/samaysetu-0.0.1-SNAPSHOT.jar`

4. Click **"Configure more options"** → **Software** → **Edit**

5. Add environment variables:
   ```
   SPRING_DATASOURCE_URL = jdbc:postgresql://db.tehdpecquwvgwpombtbl.supabase.co:5432/postgres
   SPRING_DATASOURCE_USERNAME = postgres
   SPRING_DATASOURCE_PASSWORD = samaysetumitaoe
   SERVER_PORT = 5000
   ```

6. Under **Capacity** → **Edit**:
   - Environment type: **Single instance**
   - Instance type: **t2.micro** (free tier)

7. Click **"Create application"**

8. Wait 5-10 minutes ⏳

9. **Copy your backend URL**: 
   Example: `http://samaysetu-backend.us-east-1.elasticbeanstalk.com`

---

### Step 4: Deploy Frontend to AWS (10 minutes)

1. Push code to GitHub (if not already):
   ```bash
   git add .
   git commit -m "Deploy to AWS"
   git push origin main
   ```

2. Go to **AWS Console** → **AWS Amplify**

3. Click **"New app"** → **"Host web app"**

4. Select **GitHub** and authorize

5. Select your repository and branch (`main`)

6. Configure:
   - App name: `samaysetu-frontend`
   - Root directory: `Frontend`

7. Add environment variable:
   - Key: `VITE_API_URL`
   - Value: `http://samaysetu-backend.us-east-1.elasticbeanstalk.com`
   (Use your actual backend URL from Step 3)

8. Click **"Save and deploy"**

9. Wait 5-10 minutes ⏳

10. **Your app is live!** 🎉
    URL: `https://main.d1234567890.amplifyapp.com`

---

### Step 5: Test Everything (5 minutes)

1. Open your Amplify URL
2. Login with: `admin@mitaoe.ac.in` / `admin123`
3. Check dashboard loads
4. Test a few features

✅ **Done! Share the URL with your professor!**

---

## 📚 Documentation Files

| File | Purpose |
|------|---------|
| **QUICK_AWS_DEPLOY.md** | Step-by-step deployment guide |
| **DEPLOYMENT_CHECKLIST.md** | Checklist format for deployment |
| **SWITCH_TO_PRODUCTION.md** | How Spring profiles work |
| **AWS_SERVICES_COMPARISON.md** | Why we chose Elastic Beanstalk + Amplify |
| **AWS_DEPLOYMENT_GUIDE.md** | Detailed guide with alternatives |

---

## 🔄 How to Update Later

### Update Backend:
1. Make your changes
2. Change `spring.profiles.active=prod`
3. Build: `mvnw.cmd clean package -DskipTests`
4. Go to Elastic Beanstalk → Upload new JAR
5. Done! ✅

### Update Frontend:
1. Make your changes
2. Push to GitHub: `git push origin main`
3. Amplify automatically rebuilds
4. Done! ✅ (5-10 minutes)

---

## 💡 Key Features

### Spring Profiles
- **Dev profile** (`application-dev.properties`): For local development
- **Prod profile** (`application-prod.properties`): For AWS deployment
- Switch by changing **one line** in `application.properties`

### Environment Variables
- Backend uses environment variables from AWS Elastic Beanstalk
- Frontend uses `VITE_API_URL` from AWS Amplify
- No hardcoded URLs in code!

### Auto-Deploy
- Frontend: Push to GitHub → Amplify auto-deploys
- Backend: Upload JAR → Elastic Beanstalk deploys

---

## 💰 Cost

**Free for 12 months** (AWS free tier)

After free tier:
- Backend: ~$8/month
- Frontend: ~$5/month
- **Total: ~$13/month**

Your $200 credits will last **15+ months**!

---

## 🎓 For Your Professor

**What they get**:
- Clean URL: `https://samaysetu.amplifyapp.com`
- No setup required
- Just open and use
- Professional interface
- Fast and reliable

**Login**:
- Email: `admin@mitaoe.ac.in`
- Password: `admin123`

---

## 🐛 Troubleshooting

### Backend not starting?
- Check environment variables in Elastic Beanstalk
- View logs: Elastic Beanstalk → Logs → Request Logs

### Frontend can't connect?
- Verify `VITE_API_URL` in Amplify environment variables
- Check backend URL is correct (no trailing slash)

### Login not working?
- Check Supabase database is accessible
- Verify admin user exists
- Check browser console for errors

---

## ✅ Success Checklist

- [ ] Changed `spring.profiles.active=prod`
- [ ] Built JAR file
- [ ] Deployed backend to Elastic Beanstalk
- [ ] Backend health check passing
- [ ] Deployed frontend to Amplify
- [ ] Frontend build successful
- [ ] Can login and access dashboard
- [ ] Shared URL with professor

---

## 🎉 You're Done!

Your project is now:
- ✅ Live on AWS
- ✅ Accessible via URL
- ✅ Auto-updating (frontend)
- ✅ Using free tier
- ✅ Ready for demo

**No Docker issues, no complex setup, just a URL!** 🚀

---

## 📞 Quick Links

- **AWS Console**: https://console.aws.amazon.com
- **Elastic Beanstalk**: AWS Console → Elastic Beanstalk
- **Amplify**: AWS Console → AWS Amplify
- **Supabase**: https://supabase.com/dashboard

---

## 🔥 Pro Tips

1. **Set up billing alerts** in AWS to monitor costs
2. **Bookmark your Amplify URL** for quick access
3. **Save backend URL** in a text file
4. **Test locally first** before deploying
5. **Use Git tags** for version tracking

---

**Ready to deploy? Follow QUICK_AWS_DEPLOY.md!** 🚀
