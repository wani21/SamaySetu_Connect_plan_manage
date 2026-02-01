# AWS App Runner Deployment Guide

AWS App Runner is the simplest way to deploy containerized applications on AWS. It automatically builds from source code and handles all infrastructure.

## Prerequisites
- AWS Account with free tier
- GitHub repository with your code
- Supabase database (already configured)

## Deployment Steps (15-20 minutes)

### Step 1: Prepare Your Repository

Your Dockerfile is already configured correctly:
```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY samaysetu-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENV SERVER_PORT=8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Step 2: Push to GitHub

1. Commit all changes:
```bash
git add .
git commit -m "Prepare for App Runner deployment"
git push origin main
```

### Step 3: Create App Runner Service

1. Go to AWS Console → Search "App Runner"
2. Click "Create service"

#### Source Configuration:
- **Source**: Repository
- **Repository type**: GitHub
- Click "Add new" to connect your GitHub account
- Select your repository: `SamaySetu`
- **Branch**: `main`
- **Deployment trigger**: Automatic (deploys on every push)

#### Build Configuration:
- **Configuration file**: Use a configuration file
- Click "Create apprunner.yaml"

We'll create this file in the next step.

#### Service Configuration:
- **Service name**: `samaysetu-backend`
- **Virtual CPU**: 1 vCPU
- **Memory**: 2 GB
- **Port**: 8080

#### Environment Variables:
Add these:
- `SPRING_DATASOURCE_URL` = `jdbc:postgresql://db.tehdpecquwvgwpombtbl.supabase.co:5432/postgres`
- `SPRING_DATASOURCE_USERNAME` = `postgres`
- `SPRING_DATASOURCE_PASSWORD` = `samaysetumitaoe`
- `SPRING_PROFILES_ACTIVE` = `prod`
- `PORT` = `8080`

#### Health Check:
- **Health check protocol**: HTTP
- **Health check path**: `/actuator/health`
- **Interval**: 10 seconds
- **Timeout**: 5 seconds
- **Healthy threshold**: 1
- **Unhealthy threshold**: 5

### Step 4: Create apprunner.yaml

Create this file in your repository root:

```yaml
version: 1.0
runtime: java17
build:
  commands:
    pre-build:
      - cd Backend
      - ./mvnw clean package -DskipTests
    build:
      - echo "Build completed"
  
run:
  runtime-version: 17
  command: java -jar Backend/target/samaysetu-0.0.1-SNAPSHOT.jar
  network:
    port: 8080
  env:
    - name: SERVER_PORT
      value: "8080"
```

### Step 5: Deploy

1. Click "Create & deploy"
2. Wait 5-10 minutes for the first deployment
3. App Runner will:
   - Clone your repository
   - Build the JAR file
   - Create a Docker container
   - Deploy and start your application
   - Provide a URL like: `https://xxxxx.us-east-1.awsapprunner.com`

### Step 6: Test Your Deployment

Once deployed, test these endpoints:
- Health check: `https://your-app-url.awsapprunner.com/actuator/health`
- API: `https://your-app-url.awsapprunner.com/api/auth/login`

## Advantages of App Runner

✅ **Automatic builds** from GitHub
✅ **Auto-scaling** based on traffic
✅ **HTTPS** included by default
✅ **No server management** required
✅ **Pay only for what you use**
✅ **Automatic deployments** on git push
✅ **Built-in load balancing**
✅ **Health checks** and monitoring

## Cost Estimate

**Free Tier (First 3 months):**
- 2,000 build minutes/month
- 100 GB/month data transfer
- 4 GB/month memory usage

**After Free Tier:**
- ~$25-30/month for 1 vCPU, 2 GB RAM
- Much cheaper than EC2 + Load Balancer

## Troubleshooting

### Build Fails
- Check that `mvnw` has execute permissions
- Verify `pom.xml` is in the Backend folder
- Check build logs in App Runner console

### Application Won't Start
- Verify environment variables are set correctly
- Check application logs in App Runner console
- Ensure port 8080 is exposed

### Database Connection Issues
- Verify Supabase URL is correct
- Check that Supabase allows connections from AWS
- Test database credentials

## Alternative: Deploy with Docker

If you prefer to build Docker images locally:

1. Build and push to Amazon ECR:
```bash
aws ecr create-repository --repository-name samaysetu-backend
docker build -t samaysetu-backend Backend/
docker tag samaysetu-backend:latest <account-id>.dkr.ecr.ap-south-1.amazonaws.com/samaysetu-backend:latest
docker push <account-id>.dkr.ecr.ap-south-1.amazonaws.com/samaysetu-backend:latest
```

2. In App Runner, select "Container registry" as source
3. Point to your ECR image

## Next Steps

1. **Custom Domain**: Add your own domain in App Runner settings
2. **CI/CD**: Already configured with automatic deployments
3. **Monitoring**: Use CloudWatch for logs and metrics
4. **Scaling**: Configure auto-scaling rules if needed

## Support

If you encounter issues:
1. Check App Runner logs in AWS Console
2. Verify all environment variables
3. Test database connection from AWS
4. Check application logs for errors
