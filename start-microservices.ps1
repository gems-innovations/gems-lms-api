param(
    [ValidateSet("all", "api-gateway", "ms-auth", "ms-admin", "ms-education")]
    [string]$Microservice = "all"
)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

function Import-DotEnv {
    param([string]$Path)
    if (-not (Test-Path -LiteralPath $Path)) {
        throw "Falta el archivo .env. Consulta el README para crearlo."
    }
    Get-Content -LiteralPath $Path | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#") -and $line -match '^([A-Za-z_][A-Za-z0-9_]*)=(.*)$') {
            $value = $matches[2].Trim()
            if ($value.Length -ge 2 -and (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'")))) {
                $value = $value.Substring(1, $value.Length - 2)
            }
            [Environment]::SetEnvironmentVariable($matches[1], $value, "Process")
        }
    }
}

function Assert-Java24 {
    $java = Get-Command java -ErrorAction SilentlyContinue
    if (-not $java) { throw "Java 24 o superior no esta disponible en PATH." }
    # java writes its version to stderr; cmd redirects it without turning the
    # expected output into a terminating PowerShell NativeCommandError.
    $versionText = (& cmd.exe /d /c "java -version 2>&1" | Select-Object -First 1) -join ""
    if ($versionText -notmatch 'version "(\d+)') { throw "No se pudo identificar la version de Java: $versionText" }
    if ([int]$matches[1] -lt 24) { throw "Se requiere Java 24 o superior; se encontro Java $($matches[1])." }
    Write-Host "Java $($matches[1]) detectado." -ForegroundColor Cyan
}

function Get-ConfiguredPort {
    param([string]$Value, [int]$Default)
    if ([string]::IsNullOrWhiteSpace($Value)) { return $Default }
    return [int]$Value
}

function Test-Port {
    param([int]$Port)
    try {
        $connection = New-Object System.Net.Sockets.TcpClient
        $result = $connection.BeginConnect("127.0.0.1", $Port, $null, $null)
        $open = $result.AsyncWaitHandle.WaitOne(500) -and $connection.Connected
        $connection.Close()
        return $open
    } catch { return $false }
}

function Start-GemsService {
    param([string]$Name, [int]$Port, [string]$Task, [bool]$Background)
    if (Test-Port $Port) {
        Write-Host "$Name ya responde en el puerto $Port." -ForegroundColor DarkGray
        return
    }
    if (-not $Background) {
        & .\gradlew.bat $Task
        return
    }
    New-Item -ItemType Directory -Force -Path logs | Out-Null
    $stdout = Join-Path $PSScriptRoot "logs\$Name.log"
    $stderr = Join-Path $PSScriptRoot "logs\$Name-error.log"
    Start-Process -FilePath ".\gradlew.bat" -ArgumentList $Task -WorkingDirectory $PSScriptRoot `
        -RedirectStandardOutput $stdout -RedirectStandardError $stderr -WindowStyle Hidden | Out-Null
    Write-Host "$Name iniciando en el puerto $Port (logs\$Name.log)." -ForegroundColor Green
}

Import-DotEnv (Join-Path $PSScriptRoot ".env")
Assert-Java24

if ([string]::IsNullOrWhiteSpace($env:MAIL_HOST)) { $env:MAIL_HOST = "localhost" }
if ([string]::IsNullOrWhiteSpace($env:MAIL_PORT)) { $env:MAIL_PORT = "1025" }
if ([string]::IsNullOrWhiteSpace($env:MAIL_STARTTLS)) { $env:MAIL_STARTTLS = "false" }

$services = @{
    "api-gateway" = @{ Port = 8080; Task = ":api-gateway:bootRun" }
    "ms-auth" = @{ Port = (Get-ConfiguredPort $env:AUTH_PORT 8081); Task = ":ms-auth:bootRun" }
    "ms-admin" = @{ Port = (Get-ConfiguredPort $env:ADMIN_PORT 8082); Task = ":ms-admin:bootRun" }
    "ms-education" = @{ Port = (Get-ConfiguredPort $env:EDUCATION_PORT 8083); Task = ":ms-education:bootRun" }
}

if ($Microservice -eq "all") {
    foreach ($name in @("ms-auth", "ms-admin", "ms-education", "api-gateway")) {
        $service = $services[$name]
        Start-GemsService $name $service.Port $service.Task $true
    }
    Write-Host "Servicios iniciados en segundo plano." -ForegroundColor Green
} else {
    $service = $services[$Microservice]
    Start-GemsService $Microservice $service.Port $service.Task $false
}
