# Simple script to kill processes on specific ports
Write-Host "Killing microservices on ports..." -ForegroundColor Red

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

$ports = @($apiGatewayPort, $educationPort, $authPort, $adminPort)

Write-Host "Killing processes on ports: $($ports -join ', ')" -ForegroundColor Yellow

# Kill processes on each port
foreach ($port in $ports) {
    try {
        $connections = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
        if ($connections) {
            $processes = $connections | Select-Object -ExpandProperty OwningProcess -Unique
            foreach ($pid in $processes) {
                if ($pid) {
                    Write-Host "Killing process on port $port (PID: $pid)" -ForegroundColor Red
                    Stop-Process -Id $pid -Force -ErrorAction SilentlyContinue
                    Write-Host "Port $port freed" -ForegroundColor Green
                }
            }
        } else {
            Write-Host "No process on port $port" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "Error on port $port : $($_.Exception.Message)" -ForegroundColor Red
    }
}

# Kill all Java processes
Write-Host "`nKilling all Java processes..." -ForegroundColor Cyan
Get-Process -Name "java" -ErrorAction SilentlyContinue | ForEach-Object {
    Write-Host "Killing Java process (PID: $($_.Id))" -ForegroundColor Red
    Stop-Process -Id $_.Id -Force -ErrorAction SilentlyContinue
}

Write-Host "`nDone!" -ForegroundColor Green