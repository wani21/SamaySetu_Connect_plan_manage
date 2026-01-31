# ============================================
# MySQL Database Backup Script for PowerShell
# ============================================

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "SamaySetu MySQL Database Backup" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# Configuration
$DB_NAME = "samaysetu"
$DB_USER = "root"
$BACKUP_DIR = "mysql_backups"
$TIMESTAMP = Get-Date -Format "yyyyMMdd_HHmmss"

# Create backup directory
if (-not (Test-Path $BACKUP_DIR)) {
    New-Item -ItemType Directory -Path $BACKUP_DIR | Out-Null
    Write-Host "Created backup directory: $BACKUP_DIR" -ForegroundColor Green
}

Write-Host "Database: $DB_NAME" -ForegroundColor Yellow
Write-Host "User: $DB_USER" -ForegroundColor Yellow
Write-Host "Timestamp: $TIMESTAMP" -ForegroundColor Yellow
Write-Host ""

# Prompt for password
$password = Read-Host "Enter MySQL root password" -AsSecureString
$BSTR = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($password)
$plainPassword = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR)

try {
    # Full database backup
    Write-Host "[1/3] Creating full database backup..." -ForegroundColor Cyan
    $fullBackupFile = "$BACKUP_DIR\samaysetu_full_$TIMESTAMP.sql"
    $process = Start-Process -FilePath "mysqldump" -ArgumentList "-u$DB_USER -p$plainPassword $DB_NAME" -RedirectStandardOutput $fullBackupFile -NoNewWindow -Wait -PassThru
    
    if ($process.ExitCode -eq 0) {
        Write-Host "✓ Full backup created: $fullBackupFile" -ForegroundColor Green
    } else {
        Write-Host "✗ Full backup failed!" -ForegroundColor Red
        exit 1
    }
    Write-Host ""

    # Data only backup
    Write-Host "[2/3] Creating data-only backup..." -ForegroundColor Cyan
    $dataBackupFile = "$BACKUP_DIR\samaysetu_data_$TIMESTAMP.sql"
    $process = Start-Process -FilePath "mysqldump" -ArgumentList "-u$DB_USER -p$plainPassword --no-create-info $DB_NAME" -RedirectStandardOutput $dataBackupFile -NoNewWindow -Wait -PassThru
    
    if ($process.ExitCode -eq 0) {
        Write-Host "✓ Data-only backup created: $dataBackupFile" -ForegroundColor Green
    } else {
        Write-Host "✗ Data-only backup failed!" -ForegroundColor Red
    }
    Write-Host ""

    # Schema only backup
    Write-Host "[3/3] Creating schema-only backup..." -ForegroundColor Cyan
    $schemaBackupFile = "$BACKUP_DIR\samaysetu_schema_$TIMESTAMP.sql"
    $process = Start-Process -FilePath "mysqldump" -ArgumentList "-u$DB_USER -p$plainPassword --no-data $DB_NAME" -RedirectStandardOutput $schemaBackupFile -NoNewWindow -Wait -PassThru
    
    if ($process.ExitCode -eq 0) {
        Write-Host "✓ Schema-only backup created: $schemaBackupFile" -ForegroundColor Green
    } else {
        Write-Host "✗ Schema-only backup failed!" -ForegroundColor Red
    }
    Write-Host ""

    # Show summary
    Write-Host "============================================" -ForegroundColor Cyan
    Write-Host "Backup completed successfully!" -ForegroundColor Green
    Write-Host "============================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Backup files created in: $BACKUP_DIR" -ForegroundColor Yellow
    Write-Host ""
    
    Get-ChildItem -Path $BACKUP_DIR -Filter "*$TIMESTAMP*" | ForEach-Object {
        $sizeKB = [math]::Round($_.Length / 1KB, 2)
        Write-Host "  $($_.Name) - $sizeKB KB" -ForegroundColor White
    }
    Write-Host ""

    Write-Host "============================================" -ForegroundColor Cyan
    Write-Host "Next Steps:" -ForegroundColor Yellow
    Write-Host "1. Verify backup files in $BACKUP_DIR" -ForegroundColor White
    Write-Host "2. Use these files for Supabase migration" -ForegroundColor White
    Write-Host "3. Keep backups in a safe location" -ForegroundColor White
    Write-Host "============================================" -ForegroundColor Cyan

} catch {
    Write-Host "Error occurred: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Press any key to continue..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
