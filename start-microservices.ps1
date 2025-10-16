# PowerShell script to start all microservices in parallel or a specific one
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
                [Environment]::SetEnvironmentVariable($name, $value, "Process")
            }
        }
    } else {
        Write-Host "No .env file found, using default values..." -ForegroundColor Yellow
    }
}

# Load environment variables from .env file
Load-EnvFile ".env"

# Set ports from environment variables or use defaults
$educationPort = if ($env:EDUCATION_PORT) { $env:EDUCATION_PORT }
$authPort = if ($env:AUTH_PORT) { $env:AUTH_PORT }
$adminPort = if ($env:ADMIN_PORT) { $env:ADMIN_PORT }
$apiGatewayPort = if ($env:API_GATEWAY_PORT) { $env:API_GATEWAY_PORT }

# Function to start a specific microservice
function Start-Microservice {
    param(
        [string]$Name,
        [string]$Port,
        [string]$GradleTask
    )
    
    Write-Host "Starting $Name on port: $Port" -ForegroundColor Yellow
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PWD'; `$env:${Name.ToUpper()}_PORT='$Port'; ./gradlew $GradleTask" -WindowStyle Normal
}

# Start microservices based on parameter
switch ($Microservice.ToLower()) {
    "all" {
        Write-Host "Starting all microservices..." -ForegroundColor Green
        Start-Microservice "API_GATEWAY" $apiGatewayPort ":api-gateway:bootRun"
        Start-Microservice "EDUCATION" $educationPort ":ms-education:bootRun"
        Start-Microservice "AUTH" $authPort ":ms-auth:bootRun"
        Start-Microservice "ADMIN" $adminPort ":ms-admin:bootRun"
        Write-Host "All microservices are starting in separate windows..." -ForegroundColor Green
    }
    "ms-auth" {
        Write-Host "Starting Auth microservice..." -ForegroundColor Green
        Start-Microservice "AUTH" $authPort ":ms-auth:bootRun"
    }
    "ms-education" {
        Write-Host "Starting Education microservice..." -ForegroundColor Green
        Start-Microservice "EDUCATION" $educationPort ":ms-education:bootRun"
    }
    "ms-admin" {
        Write-Host "Starting Admin microservice..." -ForegroundColor Green
        Start-Microservice "ADMIN" $adminPort ":ms-admin:bootRun"
    }
    "api-gateway" {
        Write-Host "Starting API Gateway..." -ForegroundColor Green
        Start-Microservice "API_GATEWAY" $apiGatewayPort ":api-gateway:bootRun"
    }
    default {
        Write-Host "Unknown microservice: $Microservice" -ForegroundColor Red
        Write-Host "Available options: all, ms-auth, ms-education, ms-admin, api-gateway" -ForegroundColor Yellow
        exit 1
    }
}
