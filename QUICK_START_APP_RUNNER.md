# Quick Start: Deploy to AWS App Runner (15 minutes)

## Step 1: Push to GitHub (2 minutes)

```bash
git add .
git commit -m "Add App Runner configuration"
git push origin main
```

## Step 2: Create App Runner Service (10 minutes)

1. **Go to AWS Console** → Search "App Runner" → Click "Create service"

2. **Source and deployment**:
   - Source: `Repository`
   - Repository type: `GitHub`
   - Click "Add new" → Authorize GitHub
   - Repository: Select `SamaySetu`
   - Branch: `main`
   - Deployment trigger: `Automatic`
   - Click "Next"

3. **Build settings**:
   - Configuration file: `Use a configuration file`
   - Configuration file: `apprunner.yaml`
   - Click "Next"

4. **Service settings**:
   - Service name: `samaysetu-backend`
   - Virtual CPU: `1 vCPU`
   - Memory: `2 GB`
   - Port: `8080`
   
5. **Environment variables** (Click "Add environment variable" for each):
   ```
   SPRING_DATASOURCE_URL = jdbc:postgresql://db.tehdpecquwvgwpombtbl.supabase.co:5432/postgres
   SPRING_DATASOURCE_USERNAME = postgres
   SPRING_DATASOURCE_PASSWORD = samaysetumitaoe
   SPRING_PROFILES_ACTIVE = prod
   PORT = 8080
   ```

6. **Health check**:
   - Protocol: `HTTP`
   - Path: `/actuator/health`
   - Interval: `10` seconds
   - Timeout: `5` seconds
   - Healthy threshold: `1`
   - Unhealthy threshold: `5`

7. **Security**:
   - Instance role: `Create new service role`
   - Click "Next"

8. **Review and create**:
   - Review all settings
   - Click "Create & deploy"

## Step 3: Wait for Deployment (5-8 minutes)

Watch the deployment progress:
- Building source code
- Creating container
- Deploying application
- Running health checks

## Step 4: Get Your URL

Once deployed, you'll see:
- **Default domain**: `https://xxxxx.us-east-1.awsapprunner.com`
- **Status**: Running ✅

## Step 5: Test Your API (1 minute)

Test these endpoints:
```bash
# Health check
curl https://your-app-url.awsapprunner.com/actuator/health

# Should return: {"status":"UP"}
```

## Done! 🎉

Your backend is now:
- ✅ Running on AWS App Runner
- ✅ Auto-deploying on git push
- ✅ Connected to Supabase
- ✅ HTTPS enabled
- ✅ Auto-scaling enabled

## Next: Deploy Frontend to AWS Amplify

1. Go to AWS Amplify Console
2. Click "New app" → "Host web app"
3. Connect your GitHub repository
4. Select the `Frontend` folder
5. Add environment variable:
   ```
   VITE_API_URL = https://your-app-runner-url.awsapprunner.com
   ```
6. Deploy!

## Troubleshooting

**Build fails?**
- Check logs in App Runner console
- Verify `mvnw` has correct permissions

**App won't start?**
- Check environment variables are set
- View application logs in App Runner

**403/404 errors?**
- Verify health check path is correct
- Check application is listening on port 8080

## Cost

**Free tier (3 months):**
- 2,000 build minutes
- 100 GB data transfer
- 4 GB memory

**After free tier:**
- ~$25-30/month for 1 vCPU, 2 GB RAM
