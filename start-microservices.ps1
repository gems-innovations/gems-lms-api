param(
    [string]$Microservice = "all"
)

Write-Host "Starting microservices..." -ForegroundColor Green

# Function to load .env file
function Load-EnvFile {
    param([string]$FilePath)
    if (Test-Path $FilePath) {
        Write-Host "Loading environment variables from .env file..." -ForegroundColor Cyan
        Get-Content $FilePath | ForEach-Object {
            if ($_ -match '^([^#][^=]+)=(.*)$') {
                $name = $matches[1].Trim()
                $value = $matches[2].Trim()
                if ($value -match '^["''](.*)["'']$') { $value = $matches[1] }
                [Environment]::SetEnvironmentVariable($name, $value, "Process")
            }
        }
    } else {
        Write-Host "No .env file found, using default values..." -ForegroundColor Yellow
    }
}

Load-EnvFile ".env"

# Configure Java Home
$java24Home = "C:\Users\Lu\JDK-24\jdk-24.0.2+12"
$java24Bin = "C:\Users\Lu\JDK-24\jdk-24.0.2+12\bin"

if (-not (Test-Path $java24Home)) {
    Write-Error "Java 24 is not installed in the expected directory ($java24Home)."
    exit 1
}

$env:JAVA_HOME = $java24Home
$env:PATH = "$java24Bin;" + $env:PATH
Write-Host "Using JAVA_HOME: $env:JAVA_HOME" -ForegroundColor Cyan

# Ports configurations
$gatewayPort = "8080"
$educationPort = "8081"
$authPort = "8082"
$adminPort = "8083"

function Start-ServiceProcess {
    param(
        [string]$Name,
        [string]$Port,
        [string]$GradleTask,
        [bool]$Background = $true
    )
    
    Write-Host "Starting $Name on port: $Port" -ForegroundColor Yellow
    
    if ($Background) {
        Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PWD'; `$env:JAVA_HOME='$java24Home'; `$env:PATH='$java24Bin;'+`$env:PATH; Get-Content .env | ForEach-Object { if (`$_ -match '^\s*([^#][^=]+)=(.*)$') { [System.Environment]::SetEnvironmentVariable(`$matches[1].Trim(), `$matches[2].Trim(), 'Process') } }; ./gradlew.bat $GradleTask" -WindowStyle Normal
    } else {
        Get-Content .env | ForEach-Object { if ($_ -match '^\s*([^#][^=]+)=(.*)$') { [System.Environment]::SetEnvironmentVariable($matches[1].Trim(), $matches[2].Trim(), 'Process') } }
        ./gradlew.bat $GradleTask
    }
}

switch ($Microservice.ToLower()) {
    "all" {
        Write-Host "Starting all microservices (Gateway + Auth + Admin + Education)..." -ForegroundColor Green
        Start-ServiceProcess "GATEWAY" $gatewayPort ":api-gateway:bootRun" $true
        Start-Sleep -Seconds 2
        Start-ServiceProcess "AUTH" $authPort ":ms-auth:bootRun" $true
        Start-Sleep -Seconds 2
        Start-ServiceProcess "ADMIN" $adminPort ":ms-admin:bootRun" $true
        Start-Sleep -Seconds 2
        Start-ServiceProcess "EDUCATION" $educationPort ":ms-education:bootRun" $true
        Write-Host "All microservices are starting in separate windows..." -ForegroundColor Green
    }
    "api-gateway" {
        Write-Host "Starting API Gateway microservice in foreground..." -ForegroundColor Green
        Start-ServiceProcess "GATEWAY" $gatewayPort ":api-gateway:bootRun" $false
    }
    "ms-auth" {
        Write-Host "Starting Auth microservice in foreground..." -ForegroundColor Green
        Start-ServiceProcess "AUTH" $authPort ":ms-auth:bootRun" $false
    }
    "ms-education" {
        Write-Host "Starting Education microservice in foreground..." -ForegroundColor Green
        Start-ServiceProcess "EDUCATION" $educationPort ":ms-education:bootRun" $false
    }
    "ms-admin" {
        Write-Host "Starting Admin microservice in foreground..." -ForegroundColor Green
        Start-ServiceProcess "ADMIN" $adminPort ":ms-admin:bootRun" $false
    }
    default {
        Write-Error "Unknown microservice: $Microservice"
        Write-Host "Available options: all, api-gateway, ms-auth, ms-education, ms-admin" -ForegroundColor Yellow
        exit 1
    }
}
