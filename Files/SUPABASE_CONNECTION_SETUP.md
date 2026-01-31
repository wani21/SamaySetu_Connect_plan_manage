# Supabase Connection Setup Guide

## Overview
This guide explains how to properly configure your Spring Boot application to connect to Supabase PostgreSQL.

## Step 1: Get Supabase Connection Details

### 1.1 Login to Supabase Dashboard
1. Go to https://supabase.com
2. Login to your account
3. Select your `samaysetu` project

### 1.2 Get Database Credentials
1. Click **Settings** (gear icon in left sidebar)
2. Click **Database**
3. Scroll to **Connection String** section
4. Select **Java** tab

You'll see something like:
```
jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:6543/postgres?user=postgres.xxxxxxxxxxxxx&password=YOUR_PASSWORD
```

### 1.3 Extract Connection Details
From the connection string, extract:
- **Host**: `aws-0-ap-south-1.pooler.supabase.com`
- **Port**: `6543` (connection pooler) or `5432` (direct)
- **Database**: `postgres`
- **Username**: `postgres.xxxxxxxxxxxxx` (includes your project ref)
- **Password**: Your database password

## Step 2: Update application.properties

Open `Backend/src/main/resources/application.properties` and update:

```properties
# Supabase PostgreSQL Database Configuration
spring.datasource.url=jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:6543/postgres
spring.datasource.username=postgres.xxxxxxxxxxxxx
spring.datasource.password=YOUR_ACTUAL_PASSWORD
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA Configuration for PostgreSQL
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=none
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.jdbc.lob.non_contextual_creation=true

# Connection Pool Settings (HikariCP)
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

**Replace**:
- `xxxxxxxxxxxxx` with your actual project reference
- `YOUR_ACTUAL_PASSWORD` with your database password

## Step 3: Connection Pooler vs Direct Connection

### Connection Pooler (Recommended)
```properties
# Port 6543 - Connection Pooler (Transaction Mode)
spring.datasource.url=jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:6543/postgres
```

**Advantages**:
- Better for serverless/cloud deployments
- Handles connection limits automatically
- Recommended for production

### Direct Connection
```properties
# Port 5432 - Direct Connection
spring.datasource.url=jdbc:postgresql://db.xxxxxxxxxxxxx.supabase.co:5432/postgres
```

**Advantages**:
- Lower latency
- Better for development
- Supports all PostgreSQL features

**For Development**: Use direct connection (port 5432)  
**For Production**: Use connection pooler (port 6543)

## Step 4: SSL Configuration (Optional)

If you encounter SSL errors, add SSL parameters:

```properties
spring.datasource.url=jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:6543/postgres?sslmode=require
```

Or for more strict SSL:
```properties
spring.datasource.url=jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:6543/postgres?sslmode=verify-full
```

## Step 5: Environment Variables (Recommended for Production)

Instead of hardcoding credentials, use environment variables:

### 5.1 Update application.properties
```properties
spring.datasource.url=${SUPABASE_DB_URL}
spring.datasource.username=${SUPABASE_DB_USERNAME}
spring.datasource.password=${SUPABASE_DB_PASSWORD}
```

### 5.2 Set Environment Variables

**Windows (PowerShell)**:
```powershell
$env:SUPABASE_DB_URL="jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:6543/postgres"
$env:SUPABASE_DB_USERNAME="postgres.xxxxxxxxxxxxx"
$env:SUPABASE_DB_PASSWORD="your_password"
```

**Windows (CMD)**:
```cmd
set SUPABASE_DB_URL=jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:6543/postgres
set SUPABASE_DB_USERNAME=postgres.xxxxxxxxxxxxx
set SUPABASE_DB_PASSWORD=your_password
```

**Linux/Mac**:
```bash
export SUPABASE_DB_URL="jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:6543/postgres"
export SUPABASE_DB_USERNAME="postgres.xxxxxxxxxxxxx"
export SUPABASE_DB_PASSWORD="your_password"
```

### 5.3 Run Application with Environment Variables
```bash
mvn spring-boot:run
```

## Step 6: Test Connection

### 6.1 Build Project
```bash
cd Backend
mvn clean install
```

### 6.2 Start Application
```bash
mvn spring-boot:run
```

### 6.3 Check Logs
Look for successful connection:
```
HikariPool-1 - Starting...
HikariPool-1 - Start completed.
Started Review1Application in X.XXX seconds
```

### 6.4 Test API Endpoint
```bash
curl http://localhost:8083/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@mitaoe.ac.in","password":"admin123"}'
```

Should return JWT token if successful.

## Step 7: Supabase Service Role Key (For RLS)

If you want to use Row Level Security (RLS):

### 7.1 Get Service Role Key
1. In Supabase Dashboard, go to **Settings** → **API**
2. Copy **service_role** key (secret key)

### 7.2 Add to application.properties
```properties
# Supabase Service Role Key (for RLS bypass)
supabase.service.role.key=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 7.3 Use in Database Connection (Optional)
```properties
spring.datasource.url=jdbc:postgresql://...?options=-c%20search_path=public,auth
```

## Troubleshooting

### Error: Connection Refused
**Solution**: Check if your IP is whitelisted
- Go to Supabase Dashboard → Settings → Database
- Check **Connection Pooling** settings
- Supabase allows all IPs by default, but verify

### Error: Authentication Failed
**Solution**: Verify credentials
- Double-check username includes project reference
- Verify password is correct
- Try resetting database password in Supabase dashboard

### Error: SSL Connection Error
**Solution**: Add SSL mode to connection string
```properties
spring.datasource.url=jdbc:postgresql://...?sslmode=require
```

### Error: Too Many Connections
**Solution**: Use connection pooler (port 6543)
```properties
spring.datasource.url=jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:6543/postgres
```

### Error: Driver Not Found
**Solution**: Ensure PostgreSQL driver in pom.xml
```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

Then rebuild:
```bash
mvn clean install
```

## Connection String Examples

### Development (Direct Connection)
```properties
spring.datasource.url=jdbc:postgresql://db.abcdefghijklmnop.supabase.co:5432/postgres
spring.datasource.username=postgres.abcdefghijklmnop
spring.datasource.password=your_password_here
```

### Production (Connection Pooler)
```properties
spring.datasource.url=jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:6543/postgres
spring.datasource.username=postgres.abcdefghijklmnop
spring.datasource.password=your_password_here
```

### With SSL
```properties
spring.datasource.url=jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:6543/postgres?sslmode=require
spring.datasource.username=postgres.abcdefghijklmnop
spring.datasource.password=your_password_here
```

## Security Best Practices

1. **Never commit credentials to Git**
   - Add `application.properties` to `.gitignore`
   - Use environment variables
   - Use application-local.properties for local dev

2. **Use different credentials for dev/prod**
   - Create separate Supabase projects
   - Use Spring profiles (dev, prod)

3. **Rotate passwords regularly**
   - Change database password every 90 days
   - Update in Supabase dashboard

4. **Use service role key carefully**
   - Only use in backend
   - Never expose to frontend
   - Keep it secret

## Spring Profiles (Advanced)

### application-dev.properties
```properties
spring.datasource.url=jdbc:postgresql://db.dev-project.supabase.co:5432/postgres
spring.datasource.username=postgres.devproject
spring.datasource.password=dev_password
```

### application-prod.properties
```properties
spring.datasource.url=jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:6543/postgres
spring.datasource.username=postgres.prodproject
spring.datasource.password=${PROD_DB_PASSWORD}
```

### Run with Profile
```bash
# Development
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Production
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

## Verification Checklist

- [ ] Supabase project created
- [ ] Migration script executed successfully
- [ ] Connection details copied from Supabase dashboard
- [ ] application.properties updated with correct URL
- [ ] Username includes project reference
- [ ] Password is correct
- [ ] PostgreSQL driver in pom.xml
- [ ] Project builds successfully
- [ ] Application starts without errors
- [ ] Can connect to database
- [ ] Login API works
- [ ] Data is accessible

## Next Steps

After successful connection:
1. Test all API endpoints
2. Verify data integrity
3. Set up RLS policies (optional)
4. Configure environment variables for production
5. Set up CI/CD with Supabase credentials

---

**Connection Setup Complete!**

Your Spring Boot application is now connected to Supabase PostgreSQL.
