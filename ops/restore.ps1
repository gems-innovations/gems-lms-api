param(
  [Parameter(Mandatory = $true)][string]$BackupPath,
  [string]$ComposeFile = 'docker-compose.yml',
  [switch]$ConfirmRestore
)

$ErrorActionPreference = 'Stop'
if (-not $ConfirmRestore) { throw 'La restauración reemplaza datos. Vuelve a ejecutar con -ConfirmRestore.' }

$repo = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$compose = (Resolve-Path (Join-Path $repo $ComposeFile)).Path
$backup = (Resolve-Path $BackupPath).Path
$manifestPath = Join-Path $backup 'manifest.json'
if (-not (Test-Path $manifestPath)) { throw 'El respaldo no contiene manifest.json.' }
$manifest = Get-Content $manifestPath -Raw | ConvertFrom-Json

foreach ($entry in $manifest.files) {
  $path = Join-Path $backup $entry.name
  if (-not (Test-Path $path)) { throw "Falta $($entry.name) en el respaldo." }
  $actual = (Get-FileHash $path -Algorithm SHA256).Hash.ToLowerInvariant()
  if ($actual -ne $entry.sha256) { throw "La suma de $($entry.name) no coincide; se cancela la restauración." }
}

$databases = @(
  @{ Service = 'postgres-admin'; Container = 'gems-postgres-admin'; File = 'admin.dump' },
  @{ Service = 'postgres-auth'; Container = 'gems-postgres-auth'; File = 'auth.dump' },
  @{ Service = 'postgres-education'; Container = 'gems-postgres-education'; File = 'education.dump' }
)

Push-Location $repo
try {
  foreach ($database in $databases) {
    $temporary = "/tmp/$($database.File)"
    docker cp (Join-Path $backup $database.File) "$($database.Container):$temporary"
    if ($LASTEXITCODE -ne 0) { throw "No se pudo preparar $($database.File)." }
    docker compose -f $compose exec -T $database.Service sh -c `
      'PGPASSWORD="$POSTGRES_PASSWORD" pg_restore --clean --if-exists --no-owner --no-acl -U "$POSTGRES_USER" -d "$POSTGRES_DB" "$1"' -- $temporary
    if ($LASTEXITCODE -ne 0) { throw "No se pudo restaurar $($database.Service)." }
    docker compose -f $compose exec -T $database.Service rm -f $temporary
  }

  $uploadsArchive = Join-Path $backup 'uploads.zip'
  if (Test-Path $uploadsArchive) {
    $temporaryUploads = Join-Path ([System.IO.Path]::GetTempPath()) "gems-uploads-$([guid]::NewGuid())"
    Expand-Archive $uploadsArchive $temporaryUploads -Force
    Remove-Item (Join-Path $temporaryUploads '.backup-empty') -Force -ErrorAction SilentlyContinue
    $containerId = docker compose -f $compose ps -q ms-education 2>$null
    if ($containerId) {
      docker compose -f $compose exec -T ms-education sh -c 'rm -rf /data/uploads/*'
      docker compose -f $compose cp "$temporaryUploads/." 'ms-education:/data/uploads/'
    } else {
      $localUploads = Join-Path $repo 'ms-education\data\uploads'
      New-Item -ItemType Directory -Path $localUploads -Force | Out-Null
      Remove-Item (Join-Path $localUploads '*') -Recurse -Force -ErrorAction SilentlyContinue
      Get-ChildItem $temporaryUploads -Force | Copy-Item -Destination $localUploads -Recurse -Force
    }
    Remove-Item $temporaryUploads -Recurse -Force
  }

  Write-Output 'Restauración completada. Reinicia los microservicios y ejecuta check-health.sh.'
} finally {
  Pop-Location
}
