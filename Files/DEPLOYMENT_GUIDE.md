# 🚀 SamaySetu Deployment Guide

Complete deployment guide for SamaySetu Timetable Management System in production environment.

## 📋 Prerequisites

### System Requirements:
- **Operating System**: Ubuntu 20.04+ / CentOS 8+ / Windows Server 2019+
- **RAM**: Minimum 4GB, Recommended 8GB+
- **Storage**: Minimum 20GB free space
- **Network**: Stable internet connection for email services

### Software Requirements:
- **Java**: OpenJDK 17 or Oracle JDK 17+
- **Node.js**: Version 18+ with npm
- **MySQL**: Version 8.0+
- **Web Server**: Nginx (recommended) or Apache
- **SSL Certificate**: For HTTPS (Let's Encrypt recommended)

## 🗄️ Database Setup

### 1. Install MySQL 8.0
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install mysql-server-8.0

# CentOS/RHEL
sudo dnf install mysql-server
sudo systemctl start mysqld
sudo systemctl enable mysqld

# Secure installation
sudo mysql_secure_installation
```

### 2. Create Database and User
```sql
-- Connect to MySQL as root
mysql -u root -p

-- Create database
CREATE DATABASE samaysetu_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Create dedicated user
CREATE USER 'samaysetu_user'@'localhost' IDENTIFIED BY 'StrongPassword123!';
GRANT ALL PRIVILEGES ON samaysetu_db.* TO 'samaysetu_user'@'localhost';
FLUSH PRIVILEGES;

-- Verify connection
mysql -u samaysetu_user -p samaysetu_db
```

### 3. Run Database Migrations
```bash
# Execute the migration script
mysql -u samaysetu_user -p samaysetu_db < add_batches_and_timeslot_types.sql
```

### 4. Create Initial Admin User
```sql
-- Insert admin user (password: admin123)
INSERT INTO teachers (
    name, employee_id, email, phone, password, role, 
    specialization, is_active, is_email_verified, 
    weekly_hours_limit, created_at, updated_at
) VALUES (
    'System Administrator', 
    'ADMIN001', 
    'admin@mitaoe.ac.in', 
    '9999999999', 
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 
    'ADMIN', 
    'System Administration', 
    true, 
    true, 
    40, 
    NOW(), 
    NOW()
);
```

## ⚙️ Backend Deployment

### 1. Install Java 17
```bash
# Ubuntu/Debian
sudo apt install openjdk-17-jdk

# CentOS/RHEL
sudo dnf install java-17-openjdk-devel

# Verify installation
java -version
javac -version
```

### 2. Install Maven
```bash
# Ubuntu/Debian
sudo apt install maven

# CentOS/RHEL
sudo dnf install maven

# Verify installation
mvn -version
```

### 3. Clone and Build Backend
```bash
# Clone repository
git clone https://github.com/mitaoe/SamaySetu.git
cd SamaySetu/Backend

# Configure application.properties
cp src/main/resources/application.properties.example src/main/resources/application.properties
nano src/main/resources/application.properties
```

### 4. Production Configuration
Create `src/main/resources/application-prod.properties`:
```properties
# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/samaysetu_db
spring.datasource.username=samaysetu_user
spring.datasource.password=StrongPassword123!
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA Configuration
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

# JWT Configuration
jwt.secret=SamaySetu_Production_Secret_Key_Very_Long_And_Secure_2024
jwt.expiration=86400000

# Email Configuration
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=samaysetu.mitaoe@gmail.com
spring.mail.password=your-gmail-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# Server Configuration
server.port=8083
server.servlet.context-path=/api

# Security
server.ssl.enabled=false
server.error.include-stacktrace=never

# Logging
logging.level.com.College.timetable=INFO
logging.level.org.springframework.security=WARN
logging.file.name=/var/log/samaysetu/backend.log
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n

# File Upload
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```

### 5. Build and Deploy
```bash
# Build the application
mvn clean package -DskipTests -Pprod

# Create application directory
sudo mkdir -p /opt/samaysetu
sudo mkdir -p /var/log/samaysetu

# Copy JAR file
sudo cp target/timetable-0.0.1-SNAPSHOT.jar /opt/samaysetu/samaysetu-backend.jar

# Set permissions
sudo chown -R samaysetu:samaysetu /opt/samaysetu
sudo chown -R samaysetu:samaysetu /var/log/samaysetu
```

### 6. Create System Service
Create `/etc/systemd/system/samaysetu-backend.service`:
```ini
[Unit]
Description=SamaySetu Backend Service
After=network.target mysql.service

[Service]
Type=simple
User=samaysetu
Group=samaysetu
WorkingDirectory=/opt/samaysetu
ExecStart=/usr/bin/java -Xmx1g -Xms512m -jar samaysetu-backend.jar --spring.profiles.active=prod
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal

# Environment variables
Environment=JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
Environment=SPRING_PROFILES_ACTIVE=prod

[Install]
WantedBy=multi-user.target
```

### 7. Start Backend Service
```bash
# Create samaysetu user
sudo useradd -r -s /bin/false samaysetu

# Reload systemd and start service
sudo systemctl daemon-reload
sudo systemctl enable samaysetu-backend
sudo systemctl start samaysetu-backend

# Check status
sudo systemctl status samaysetu-backend
sudo journalctl -u samaysetu-backend -f
```

## 🎨 Frontend Deployment

### 1. Install Node.js and npm
```bash
# Ubuntu/Debian
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt-get install -y nodejs

# CentOS/RHEL
curl -fsSL https://rpm.nodesource.com/setup_18.x | sudo bash -
sudo dnf install -y nodejs

# Verify installation
node --version
npm --version
```

### 2. Build Frontend
```bash
# Navigate to frontend directory
cd ../Frontend

# Install dependencies
npm install

# Create production environment file
cat > .env.production << EOF
VITE_API_URL=https://your-domain.com/api
VITE_APP_NAME=SamaySetu
VITE_COLLEGE_NAME=MIT Academy of Engineering
EOF

# Build for production
npm run build
```

### 3. Deploy to Web Server
```bash
# Create web directory
sudo mkdir -p /var/www/samaysetu

# Copy build files
sudo cp -r dist/* /var/www/samaysetu/

# Set permissions
sudo chown -R www-data:www-data /var/www/samaysetu
sudo chmod -R 755 /var/www/samaysetu
```

## 🌐 Web Server Configuration

### Nginx Configuration
Create `/etc/nginx/sites-available/samaysetu`:
```nginx
server {
    listen 80;
    server_name your-domain.com www.your-domain.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name your-domain.com www.your-domain.com;

    # SSL Configuration
    ssl_certificate /etc/letsencrypt/live/your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES256-GCM-SHA512:DHE-RSA-AES256-GCM-SHA512:ECDHE-RSA-AES256-GCM-SHA384:DHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers off;

    # Security Headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header Referrer-Policy "no-referrer-when-downgrade" always;
    add_header Content-Security-Policy "default-src 'self' http: https: data: blob: 'unsafe-inline'" always;

    # Frontend
    location / {
        root /var/www/samaysetu;
        index index.html;
        try_files $uri $uri/ /index.html;
        
        # Cache static assets
        location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
            expires 1y;
            add_header Cache-Control "public, immutable";
        }
    }

    # Backend API
    location /api/ {
        proxy_pass http://localhost:8083/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
        
        # Timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
        
        # File upload
        client_max_body_size 10M;
    }

    # Gzip Compression
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_proxied expired no-cache no-store private must-revalidate auth;
    gzip_types
        text/plain
        text/css
        text/xml
        text/javascript
        application/javascript
        application/xml+rss
        application/json;

    # Logging
    access_log /var/log/nginx/samaysetu_access.log;
    error_log /var/log/nginx/samaysetu_error.log;
}
```

### Enable Site and Restart Nginx
```bash
# Enable site
sudo ln -s /etc/nginx/sites-available/samaysetu /etc/nginx/sites-enabled/

# Test configuration
sudo nginx -t

# Restart nginx
sudo systemctl restart nginx
```

## 🔒 SSL Certificate Setup

### Using Let's Encrypt (Recommended)
```bash
# Install Certbot
sudo apt install certbot python3-certbot-nginx

# Obtain certificate
sudo certbot --nginx -d your-domain.com -d www.your-domain.com

# Test auto-renewal
sudo certbot renew --dry-run

# Set up auto-renewal cron job
echo "0 12 * * * /usr/bin/certbot renew --quiet" | sudo crontab -
```

## 🔥 Firewall Configuration

### UFW (Ubuntu)
```bash
# Enable firewall
sudo ufw enable

# Allow SSH
sudo ufw allow ssh

# Allow HTTP and HTTPS
sudo ufw allow 'Nginx Full'

# Allow MySQL (if remote access needed)
sudo ufw allow 3306

# Check status
sudo ufw status
```

### Firewalld (CentOS/RHEL)
```bash
# Start and enable firewalld
sudo systemctl start firewalld
sudo systemctl enable firewalld

# Allow services
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-service=https
sudo firewall-cmd --permanent --add-service=ssh

# Allow MySQL port
sudo firewall-cmd --permanent --add-port=3306/tcp

# Reload firewall
sudo firewall-cmd --reload
```

## 📊 Monitoring and Logging

### 1. Log Rotation
Create `/etc/logrotate.d/samaysetu`:
```
/var/log/samaysetu/*.log {
    daily
    missingok
    rotate 30
    compress
    delaycompress
    notifempty
    create 644 samaysetu samaysetu
    postrotate
        systemctl reload samaysetu-backend
    endscript
}
```

### 2. System Monitoring
```bash
# Install monitoring tools
sudo apt install htop iotop nethogs

# Monitor backend service
sudo journalctl -u samaysetu-backend -f

# Monitor system resources
htop

# Monitor network
sudo nethogs
```

### 3. Database Monitoring
```bash
# MySQL performance monitoring
mysql -u root -p -e "SHOW PROCESSLIST;"
mysql -u root -p -e "SHOW STATUS LIKE 'Threads_connected';"

# Enable slow query log
mysql -u root -p -e "SET GLOBAL slow_query_log = 'ON';"
mysql -u root -p -e "SET GLOBAL long_query_time = 2;"
```

## 🔄 Backup Strategy

### 1. Database Backup
Create `/opt/samaysetu/backup-db.sh`:
```bash
#!/bin/bash
BACKUP_DIR="/opt/samaysetu/backups"
DATE=$(date +%Y%m%d_%H%M%S)
DB_NAME="samaysetu_db"
DB_USER="samaysetu_user"
DB_PASS="StrongPassword123!"

# Create backup directory
mkdir -p $BACKUP_DIR

# Create database backup
mysqldump -u $DB_USER -p$DB_PASS $DB_NAME > $BACKUP_DIR/samaysetu_db_$DATE.sql

# Compress backup
gzip $BACKUP_DIR/samaysetu_db_$DATE.sql

# Remove backups older than 30 days
find $BACKUP_DIR -name "*.sql.gz" -mtime +30 -delete

echo "Database backup completed: samaysetu_db_$DATE.sql.gz"
```

### 2. Application Backup
Create `/opt/samaysetu/backup-app.sh`:
```bash
#!/bin/bash
BACKUP_DIR="/opt/samaysetu/backups"
DATE=$(date +%Y%m%d_%H%M%S)

# Create backup directory
mkdir -p $BACKUP_DIR

# Backup application files
tar -czf $BACKUP_DIR/samaysetu_app_$DATE.tar.gz \
    /opt/samaysetu/samaysetu-backend.jar \
    /var/www/samaysetu \
    /etc/nginx/sites-available/samaysetu \
    /etc/systemd/system/samaysetu-backend.service

# Remove backups older than 7 days
find $BACKUP_DIR -name "samaysetu_app_*.tar.gz" -mtime +7 -delete

echo "Application backup completed: samaysetu_app_$DATE.tar.gz"
```

### 3. Automated Backups
```bash
# Make scripts executable
sudo chmod +x /opt/samaysetu/backup-db.sh
sudo chmod +x /opt/samaysetu/backup-app.sh

# Add to crontab
sudo crontab -e

# Add these lines:
# Daily database backup at 2 AM
0 2 * * * /opt/samaysetu/backup-db.sh

# Weekly application backup on Sunday at 3 AM
0 3 * * 0 /opt/samaysetu/backup-app.sh
```

## 🚀 Deployment Checklist

### Pre-Deployment:
- [ ] Server meets system requirements
- [ ] Domain name configured and DNS propagated
- [ ] SSL certificate obtained
- [ ] Database server installed and secured
- [ ] Firewall configured
- [ ] Backup strategy implemented

### Backend Deployment:
- [ ] Java 17 installed
- [ ] Maven installed
- [ ] Database created and migrated
- [ ] Application built and deployed
- [ ] System service created and started
- [ ] Logs are being generated
- [ ] Health check endpoint responding

### Frontend Deployment:
- [ ] Node.js installed
- [ ] Application built for production
- [ ] Static files deployed to web server
- [ ] Web server configured
- [ ] SSL certificate configured
- [ ] Gzip compression enabled

### Post-Deployment:
- [ ] Application accessible via domain
- [ ] Login functionality working
- [ ] Email functionality working
- [ ] File upload functionality working
- [ ] Database operations working
- [ ] Monitoring and logging configured
- [ ] Backup scripts tested
- [ ] Performance optimized

## 🔧 Maintenance

### Regular Tasks:
- **Daily**: Check application logs and system resources
- **Weekly**: Review backup integrity and system updates
- **Monthly**: Update dependencies and security patches
- **Quarterly**: Performance review and optimization

### Update Procedure:
1. **Backup** current application and database
2. **Test** updates in staging environment
3. **Deploy** during maintenance window
4. **Verify** all functionality works
5. **Monitor** for issues post-deployment

## 📞 Support

For deployment issues:
- Check application logs: `/var/log/samaysetu/backend.log`
- Check system logs: `sudo journalctl -u samaysetu-backend`
- Check web server logs: `/var/log/nginx/samaysetu_error.log`
- Monitor system resources: `htop`, `df -h`, `free -m`

---

**Deployment Guide for MIT Academy of Engineering**

© 2026 MIT Academy of Engineering - SamaySetu Development Team