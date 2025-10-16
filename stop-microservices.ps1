# Simple script to kill processes on specific ports or a specific microservice
param(
    [string]$Microservice = "all"
)

Write-Host "Stopping microservices..." -ForegroundColor Red

# Function to load .env file
function Load-EnvFile {
    param([string]$FilePath)
    
    if (Test-Path $FilePath) {
        Write-Host "Loading ports from .env file..." -ForegroundColor Cyan
        Get-Content $FilePath | ForEach-Object {
            if ($_ -match '^([^#][^=]+)=(.*)$') {
                $name = $matches[1].Trim()
                $value = $matches[2].Trim()
                [Environment]::SetEnvironmentVariable($name, $value, "Process")
            }
        }
    } else {
        Write-Host "No .env file found. Stopping script." -ForegroundColor Red
        exit 1
    }
}

# Load ports from .env
Load-EnvFile ".env"

# Get ports - fixed syntax
$apiGatewayPort = if ($env:API_GATEWAY_PORT) { $env:API_GATEWAY_PORT }
$educationPort = if ($env:EDUCATION_PORT) { $env:EDUCATION_PORT }
$authPort = if ($env:AUTH_PORT) { $env:AUTH_PORT }
$adminPort = if ($env:ADMIN_PORT) { $env:ADMIN_PORT }

# Function to kill processes on a specific port
function Stop-MicroserviceOnPort {
    param(
        [string]$Port,
        [string]$ServiceName
    )
    
    try {
        $connections = Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue
        if ($connections) {
            $processes = $connections | Select-Object -ExpandProperty OwningProcess -Unique
            foreach ($pid in $processes) {
                if ($pid) {
                    Write-Host "Killing $ServiceName on port $Port (PID: $pid)" -ForegroundColor Red
                    Stop-Process -Id $pid -Force -ErrorAction SilentlyContinue
                    Write-Host "$ServiceName stopped successfully" -ForegroundColor Green
                }
            }
        } else {
            Write-Host "No $ServiceName process found on port $Port" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "Error stopping $ServiceName on port $Port : $($_.Exception.Message)" -ForegroundColor Red
    }
}

# Stop microservices based on parameter
switch ($Microservice.ToLower()) {
    "all" {
        Write-Host "Stopping all microservices..." -ForegroundColor Red
        $ports = @(
            @{Port=$apiGatewayPort; Name="API Gateway"},
            @{Port=$educationPort; Name="Education Service"},
            @{Port=$authPort; Name="Auth Service"},
            @{Port=$adminPort; Name="Admin Service"}
        )
        
        foreach ($service in $ports) {
            Stop-MicroserviceOnPort $service.Port $service.Name
        }
        
        # Kill all Java processes as fallback
        Write-Host "`nKilling all remaining Java processes..." -ForegroundColor Cyan
        Get-Process -Name "java" -ErrorAction SilentlyContinue | ForEach-Object {
            Write-Host "Killing Java process (PID: $($_.Id))" -ForegroundColor Red
            Stop-Process -Id $_.Id -Force -ErrorAction SilentlyContinue
        }
    }
    "ms-auth" {
        Write-Host "Stopping Auth microservice..." -ForegroundColor Red
        Stop-MicroserviceOnPort $authPort "Auth Service"
    }
    "ms-education" {
        Write-Host "Stopping Education microservice..." -ForegroundColor Red
        Stop-MicroserviceOnPort $educationPort "Education Service"
    }
    "ms-admin" {
        Write-Host "Stopping Admin microservice..." -ForegroundColor Red
        Stop-MicroserviceOnPort $adminPort "Admin Service"
    }
    "api-gateway" {
        Write-Host "Stopping API Gateway..." -ForegroundColor Red
        Stop-MicroserviceOnPort $apiGatewayPort "API Gateway"
    }
    default {
        Write-Host "Unknown microservice: $Microservice" -ForegroundColor Red
        Write-Host "Available options: all, ms-auth, ms-education, ms-admin, api-gateway" -ForegroundColor Yellow
        exit 1
    }
}

Write-Host "`nDone!" -ForegroundColor Green