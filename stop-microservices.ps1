param(
    [string]$Microservice = "all"
)

Write-Host "Stopping microservices..." -ForegroundColor Red

# Function to load .env file
function Load-EnvFile {
    param([string]$FilePath)
    if (Test-Path $FilePath) {
        Get-Content $FilePath | ForEach-Object {
            if ($_ -match '^([^#][^=]+)=(.*)$') {
                $name = $matches[1].Trim()
                $value = $matches[2].Trim()
                if ($value -match '^["''](.*)["'']$') { $value = $matches[1] }
                [Environment]::SetEnvironmentVariable($name, $value, "Process")
            }
        }
    }
}

Load-EnvFile ".env"

$apiGatewayPort = $env:API_GATEWAY_PORT
$educationPort = $env:EDUCATION_PORT
$authPort = $env:AUTH_PORT
$adminPort = $env:ADMIN_PORT

function Stop-ProcessOnPort {
    param(
        [string]$Port,
        [string]$ServiceName
    )
    
    if (-not $Port) {
        Write-Host "No port configured for $ServiceName" -ForegroundColor Yellow
        return
    }
    
    Write-Host "Checking for $ServiceName on port $Port..." -ForegroundColor Cyan
    
    $conn = Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue
    if ($conn) {
        $pids = $conn.OwningProcess | Select-Object -Unique
        foreach ($pid in $pids) {
            Write-Host "Stopping process $pid for $ServiceName on port $Port..." -ForegroundColor Red
            Stop-Process -Id $pid -Force -ErrorAction SilentlyContinue
        }
        Write-Host "$ServiceName stopped successfully." -ForegroundColor Green
    } else {
        Write-Host "No process found on port $Port for $ServiceName." -ForegroundColor Yellow
    }
}

switch ($Microservice.ToLower()) {
    "all" {
        Stop-ProcessOnPort $apiGatewayPort "API Gateway"
        Stop-ProcessOnPort $educationPort "Education Service"
        Stop-ProcessOnPort $authPort "Auth Service"
        Stop-ProcessOnPort $adminPort "Admin Service"
        
        Write-Host "Stopping any remaining Java processes..." -ForegroundColor Red
        Get-Process java -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
        Write-Host "All Java processes stopped." -ForegroundColor Green
    }
    "ms-auth" {
        Stop-ProcessOnPort $authPort "Auth Service"
    }
    "ms-education" {
        Stop-ProcessOnPort $educationPort "Education Service"
    }
    "ms-admin" {
        Stop-ProcessOnPort $adminPort "Admin Service"
    }
    "api-gateway" {
        Stop-ProcessOnPort $apiGatewayPort "API Gateway"
    }
    default {
        Write-Error "Unknown microservice: $Microservice"
        Write-Host "Available options: all, ms-auth, ms-education, ms-admin, api-gateway" -ForegroundColor Yellow
        exit 1
    }
}

Write-Host "Done!" -ForegroundColor Green
