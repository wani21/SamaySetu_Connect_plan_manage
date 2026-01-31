@echo off
REM ============================================
REM MySQL Database Backup Script for Windows
REM ============================================

echo ============================================
echo SamaySetu MySQL Database Backup
echo ============================================
echo.

REM Set variables
set DB_NAME=samaysetu
set DB_USER=root
set BACKUP_DIR=mysql_backups
set TIMESTAMP=%date:~-4,4%%date:~-10,2%%date:~-7,2%_%time:~0,2%%time:~3,2%%time:~6,2%
set TIMESTAMP=%TIMESTAMP: =0%

REM Create backup directory if it doesn't exist
if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"

echo Creating backup directory: %BACKUP_DIR%
echo Database: %DB_NAME%
echo User: %DB_USER%
echo Timestamp: %TIMESTAMP%
echo.

REM Full database backup
echo [1/3] Creating full database backup...
mysqldump -u %DB_USER% -p %DB_NAME% > "%BACKUP_DIR%\samaysetu_full_%TIMESTAMP%.sql"
if %errorlevel% neq 0 (
    echo ERROR: Full backup failed!
    pause
    exit /b 1
)
echo Full backup created: %BACKUP_DIR%\samaysetu_full_%TIMESTAMP%.sql
echo.

REM Data only backup (no CREATE statements)
echo [2/3] Creating data-only backup...
mysqldump -u %DB_USER% -p --no-create-info %DB_NAME% > "%BACKUP_DIR%\samaysetu_data_%TIMESTAMP%.sql"
if %errorlevel% neq 0 (
    echo ERROR: Data-only backup failed!
    pause
    exit /b 1
)
echo Data-only backup created: %BACKUP_DIR%\samaysetu_data_%TIMESTAMP%.sql
echo.

REM Schema only backup (no data)
echo [3/3] Creating schema-only backup...
mysqldump -u %DB_USER% -p --no-data %DB_NAME% > "%BACKUP_DIR%\samaysetu_schema_%TIMESTAMP%.sql"
if %errorlevel% neq 0 (
    echo ERROR: Schema-only backup failed!
    pause
    exit /b 1
)
echo Schema-only backup created: %BACKUP_DIR%\samaysetu_schema_%TIMESTAMP%.sql
echo.

echo ============================================
echo Backup completed successfully!
echo ============================================
echo.
echo Backup files created in: %BACKUP_DIR%
echo.
dir "%BACKUP_DIR%\*%TIMESTAMP%*" /b
echo.

REM Show file sizes
echo File sizes:
for %%f in ("%BACKUP_DIR%\*%TIMESTAMP%*") do (
    echo %%~nxf - %%~zf bytes
)
echo.

echo ============================================
echo Next Steps:
echo 1. Verify backup files in %BACKUP_DIR%
echo 2. Use these files for Supabase migration
echo 3. Keep backups in a safe location
echo ============================================
echo.

pause
