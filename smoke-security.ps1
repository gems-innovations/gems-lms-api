param([string]$BaseUrl = 'http://localhost:8080/api/v1')
$ErrorActionPreference = 'Stop'
$settings = @{}
foreach ($line in Get-Content -LiteralPath (Join-Path $PSScriptRoot '.env')) {
  if ($line -match '^([A-Za-z_][A-Za-z0-9_]*)=(.*)$') { $settings[$Matches[1]] = $Matches[2] }
}
$password = $settings['DEV_PASSWORD']
if (!$password) { throw 'Configurar DEV_PASSWORD para las cuentas de desarrollo.' }
function Request([string]$method, [string]$path, $body, [string]$token = '') {
  $options = @{ Uri = "$BaseUrl$path"; Method = $method; SkipHttpErrorCheck = $true; TimeoutSec = 15; StatusCodeVariable = 'code' }
  if ($token) { $options.Headers = @{ Authorization = "Bearer $token" } }
  if ($null -ne $body) { $options.ContentType = 'application/json'; $options.Body = ($body | ConvertTo-Json -Depth 10 -Compress) }
  $reply = Invoke-RestMethod @options
  return @{ Status = $code; Body = $reply }
}
function Expect($reply, [int]$expected, [string]$label) {
  if ($reply.Status -ne $expected) { throw "$label : esperado $expected, recibido $($reply.Status)" }
  Write-Output "$label : OK ($expected)"
}
$admin = Request POST '/auth/login' @{email='admin@unal.edu.co';password=$password}
Expect $admin 200 'Login administrador'
$adminToken = $admin.Body.token
$id = $null
$quizId = $null
try {
  $email = "smoke-$([guid]::NewGuid().ToString('N'))@example.test"
  $created = Request POST '/auth/register' @{email=$email;firstName='Smoke';lastName='Session';role='STUDENT';institutionId='inst-1'} $adminToken
  Expect $created 201 'Alta temporal'
  $id = $created.Body.userId
  $login = Request POST '/auth/login' @{email=$email;password=$created.Body.temporaryPassword}
  Expect $login 200 'Login con contraseña temporal'
  Expect (Request GET '/courses' $null $login.Body.token) 403 'API bloqueada hasta cambiar contraseña'
  $newPassword = 'aB1!' + [guid]::NewGuid().ToString('N')
  Expect (Request POST '/auth/change-password' @{currentPassword=$created.Body.temporaryPassword;newPassword=$newPassword} $login.Body.token) 204 'Cambio de contraseña'
  Expect (Request GET '/courses' $null $login.Body.token) 401 'Token anterior revocado'
  $fresh = Request POST '/auth/login' @{email=$email;password=$newPassword}
  Expect $fresh 200 'Nueva sesión'
  Expect (Request GET '/courses' $null $fresh.Body.token) 200 'Acceso a cursos'
  Expect (Request GET '/institutions/inst-1' $null $fresh.Body.token) 200 'Acceso a institución'
  Expect (Request PUT "/users/$id/status" @{} $adminToken) 200 'Desactivar cuenta'
  Expect (Request GET '/courses' $null $fresh.Body.token) 401 'Revocación al desactivar'
  Expect (Request PUT "/users/$id/status" @{} $adminToken) 200 'Reactivar cuenta'
  Expect (Request GET '/courses' $null $fresh.Body.token) 401 'La reactivación no restaura tokens anteriores'
  $fresh = Request POST '/auth/login' @{email=$email;password=$newPassword}
  Expect $fresh 200 'Login tras reactivación'
  Expect (Request DELETE "/users/$id" $null $adminToken) 204 'Borrar cuenta de prueba'
  Expect (Request GET '/courses' $null $fresh.Body.token) 401 'Revocación al borrar'
  $course = Request GET '/courses/13' $null $adminToken
  $lessonId = $course.Body.modules[0].lessons[0].id
  $quiz = Request POST '/quizzes' @{lessonId=$lessonId;title='Smoke isolated quiz';passingScore=70;questions=@(@{text='Test';options=@('A','B');correctOption='A'})} $adminToken
  Expect $quiz 201 'Crear quiz en institución propia'
  $quizId = $quiz.Body.id
  $other = Request POST '/auth/login' @{email='admin@pragma.co';password=$password}
  Expect $other 200 'Login de otra institución'
  Expect (Request GET "/quizzes/$quizId" $null $other.Body.token) 403 'Quiz oculto a otra institución'
  Expect (Request DELETE "/quizzes/$quizId" $null $other.Body.token) 403 'Otra institución no puede borrar quiz'
} finally {
  if ($quizId) { Expect (Request DELETE "/quizzes/$quizId" $null $adminToken) 204 'Limpiar quiz de prueba' }
  if ($id) {
    $removed = Request DELETE "/users/$id" $null $adminToken
    if ($removed.Status -notin 204,404) { throw 'No se pudo limpiar la cuenta de prueba' }
  }
}
