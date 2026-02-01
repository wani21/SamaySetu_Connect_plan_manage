# 🚀 Quick Reference Card - AWS Deployment

## 📌 One-Page Cheat Sheet

### 🎯 Before Deployment

**1. Switch to Production**
```properties
# File: Backend/src/main/resources/application.properties
spring.profiles.active=prod  # Change from 'dev' to 'prod'
```

**2. Build JAR**
```bash
cd Backend
mvnw.cmd clean package -DskipTests
```

**3. Push to GitHub**
```bash
git add .
git commit -m "Deploy to AWS"
git push origin main
```

---

### 🔧 Backend Deployment (Elastic Beanstalk)

**AWS Console → Elastic Beanstalk → Create Application**

| Setting | Value |
|---------|-------|
| Name | `samaysetu-backend` |
| Platform | Java |
| Platform Branch | Corretto 17 |
| Upload | `Backend/target/samaysetu-0.0.1-SNAPSHOT.jar` |
| Instance Type | t2.micro (free tier) |
| Environment | Single instance |

**Environment Variables**:
```
SPRING_DATASOURCE_URL = jdbc:postgresql://db.tehdpecquwvgwpombtbl.supabase.co:5432/postgres
SPRING_DATASOURCE_USERNAME = postgres
SPRING_DATASOURCE_PASSWORD = samaysetumitaoe
SERVER_PORT = 5000
```

**Result**: `http://samaysetu-backend.elasticbeanstalk.com`

---

### 🎨 Frontend Deployment (Amplify)

**AWS Console → Amplify → New App → Host Web App**

| Setting | Value |
|---------|-------|
| Source | GitHub |
| Repository | Your repo |
| Branch | main |
| App Name | `samaysetu-frontend` |
| Root Directory | `Frontend` |

**Environment Variable**:
```
VITE_API_URL = http://samaysetu-backend.elasticbeanstalk.com
```

**Result**: `https://main.d1234567890.amplifyapp.com`

---

### ✅ Verification

1. Open frontend URL
2. Login: `admin@mitaoe.ac.in` / `admin123`
3. Check dashboard loads
4. Test features

---

### 🔄 Update Commands

**Backend**:
```bash
# 1. Change profile to prod
# 2. Build
cd Backend
mvnw.cmd clean package -DskipTests
# 3. Upload to Elastic Beanstalk
```

**Frontend**:
```bash
git push origin main
# Amplify auto-deploys in 5-10 minutes
```

---

### 🐛 Quick Troubleshooting

| Problem | Solution |
|---------|----------|
| Backend unhealthy | Check environment variables |
| Frontend can't connect | Verify `VITE_API_URL` |
| Login fails | Check Supabase connection |
| Build fails | Verify Java 17 |

---

### 💰 Cost

- **Free Tier**: $0/month (12 months)
- **After**: ~$13/month
- **Your Credits**: $200 (15+ months)

---

### 📚 Full Documentation

- `START_HERE_AWS_DEPLOYMENT.md` - Overview
- `QUICK_AWS_DEPLOY.md` - Step-by-step
- `DEPLOYMENT_CHECKLIST.md` - Checklist
- `SWITCH_TO_PRODUCTION.md` - Profiles guide

---

### 🎓 Share with Professor

**URL**: `https://main.d1234567890.amplifyapp.com`  
**Login**: `admin@mitaoe.ac.in` / `admin123`

---

**Total Time**: 30 minutes | **Cost**: $0 | **Complexity**: Low ✅
