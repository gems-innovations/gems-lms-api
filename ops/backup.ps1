param(
  [string]$ComposeFile = 'docker-compose.yml',
  [string]$OutputRoot = (Join-Path $PSScriptRoot '..\backups')
)

$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$compose = (Resolve-Path (Join-Path $repo $ComposeFile)).Path
$stamp = (Get-Date).ToUniversalTime().ToString('yyyyMMdd-HHmmssZ')
$destination = [System.IO.Path]::GetFullPath((Join-Path $OutputRoot $stamp))
New-Item -ItemType Directory -Path $destination -Force | Out-Null

$databases = @(
  @{ Service = 'postgres-admin'; Container = 'gems-postgres-admin'; File = 'admin.dump' },
  @{ Service = 'postgres-auth'; Container = 'gems-postgres-auth'; File = 'auth.dump' },
  @{ Service = 'postgres-education'; Container = 'gems-postgres-education'; File = 'education.dump' }
)

Push-Location $repo
try {
  foreach ($database in $databases) {
    $temporary = "/tmp/$($database.File)"
    docker compose -f $compose exec -T $database.Service sh -c `
      'PGPASSWORD="$POSTGRES_PASSWORD" pg_dump --format=custom --no-owner --no-acl -U "$POSTGRES_USER" -d "$POSTGRES_DB" -f "$1"' -- $temporary
    if ($LASTEXITCODE -ne 0) { throw "No se pudo respaldar $($database.Service)." }

    docker cp "$($database.Container):$temporary" (Join-Path $destination $database.File)
    if ($LASTEXITCODE -ne 0) { throw "No se pudo copiar $($database.File)." }
    docker compose -f $compose exec -T $database.Service rm -f $temporary
  }

  $uploadsArchive = Join-Path $destination 'uploads.zip'
  $containerId = docker compose -f $compose ps -q ms-education 2>$null
  $temporaryUploads = Join-Path $destination 'uploads'
  if ($containerId) {
    New-Item -ItemType Directory -Path $temporaryUploads -Force | Out-Null
    docker compose -f $compose cp 'ms-education:/data/uploads/.' $temporaryUploads
    if ($LASTEXITCODE -ne 0) { throw 'No se pudieron copiar los archivos subidos.' }
  } else {
    $localUploads = Join-Path $repo 'ms-education\data\uploads'
    New-Item -ItemType Directory -Path $temporaryUploads -Force | Out-Null
    if (Test-Path $localUploads) {
      Get-ChildItem $localUploads -Force | Copy-Item -Destination $temporaryUploads -Recurse -Force
    }
  }
  if (-not (Get-ChildItem $temporaryUploads -Force | Select-Object -First 1)) {
    Set-Content (Join-Path $temporaryUploads '.backup-empty') '' -Encoding utf8
  }
  Compress-Archive -Path (Join-Path $temporaryUploads '*') -DestinationPath $uploadsArchive -Force
  Remove-Item $temporaryUploads -Recurse -Force

  $files = Get-ChildItem $destination -File | ForEach-Object {
    [ordered]@{ name = $_.Name; bytes = $_.Length; sha256 = (Get-FileHash $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant() }
  }
  [ordered]@{
    createdAt = (Get-Date).ToUniversalTime().ToString('o')
    composeFile = (Split-Path $compose -Leaf)
    files = $files
  } | ConvertTo-Json -Depth 4 | Set-Content (Join-Path $destination 'manifest.json') -Encoding utf8

  Write-Output "Respaldo creado y verificado en $destination"
} finally {
  Pop-Location
}
