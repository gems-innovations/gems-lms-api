param(
    [ValidateSet("all", "api-gateway", "ms-auth", "ms-admin", "ms-education")]
    [string]$Microservice = "all"
)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

function Import-DotEnv {
    param([string]$Path)
    if (-not (Test-Path -LiteralPath $Path)) { return }
    Get-Content -LiteralPath $Path | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#") -and $line -match '^([A-Za-z_][A-Za-z0-9_]*)=(.*)$') {
            $value = $matches[2].Trim().Trim('"').Trim("'")
            [Environment]::SetEnvironmentVariable($matches[1], $value, "Process")
        }
    }
}

function Get-ConfiguredPort {
    param([string]$Value, [int]$Default)
    if ([string]::IsNullOrWhiteSpace($Value)) { return $Default }
    return [int]$Value
}

function Stop-GemsService {
    param([string]$Name, [int]$Port)
    $listeners = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue
    $processIds = @($listeners | Select-Object -ExpandProperty OwningProcess -Unique)
    if (-not $processIds.Count) {
        Write-Host "$Name no esta activo en el puerto $Port." -ForegroundColor DarkGray
        return
    }
    foreach ($processId in $processIds) {
        Stop-Process -Id $processId -Force -ErrorAction Stop
        Write-Host "$Name detenido (puerto $Port)." -ForegroundColor Green
    }
}

Import-DotEnv (Join-Path $PSScriptRoot ".env")
$services = @{
    "api-gateway" = 8080
    "ms-auth" = Get-ConfiguredPort $env:AUTH_PORT 8081
    "ms-admin" = Get-ConfiguredPort $env:ADMIN_PORT 8082
    "ms-education" = Get-ConfiguredPort $env:EDUCATION_PORT 8083
}

if ($Microservice -eq "all") {
    foreach ($name in @("api-gateway", "ms-education", "ms-admin", "ms-auth")) {
        Stop-GemsService $name $services[$name]
    }
} else {
    Stop-GemsService $Microservice $services[$Microservice]
}
