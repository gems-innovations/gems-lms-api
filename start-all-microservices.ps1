# PowerShell script to start all microservices in parallel
Write-Host "Starting all microservices in parallel..." -ForegroundColor Green

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

Write-Host "API Gateway will run on port: $apiGatewayPort" -ForegroundColor Yellow
Write-Host "Education service will run on port: $educationPort" -ForegroundColor Yellow
Write-Host "Auth service will run on port: $authPort" -ForegroundColor Yellow
Write-Host "Admin service will run on port: $adminPort" -ForegroundColor Yellow

# Start each microservice in a separate PowerShell window
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PWD'; `$env:API_GATEWAY_PORT='$apiGatewayPort'; ./gradlew :api-gateway:bootRun" -WindowStyle Normal
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PWD'; `$env:EDUCATION_PORT='$educationPort'; ./gradlew :ms-education:bootRun" -WindowStyle Normal
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PWD'; `$env:AUTH_PORT='$authPort'; ./gradlew :ms-auth:bootRun" -WindowStyle Normal
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PWD'; `$env:ADMIN_PORT='$adminPort'; ./gradlew :ms-admin:bootRun" -WindowStyle Normal

Write-Host "All microservices are starting in separate windows..." -ForegroundColor Green
