Write-Host "=============================================================" -ForegroundColor Cyan
Write-Host "   GEMS LMS - SUITE COMPLETA DE PRUEBAS END-TO-END (GATEWAY: 8080)" -ForegroundColor Cyan
Write-Host "=============================================================" -ForegroundColor Cyan

$baseUrl = "http://localhost:8080"
$global:passedCount = 0
$global:failedCount = 0

function Assert-Result {
    param(
        [string]$TestName,
        [int]$ExpectedStatus,
        [int]$ActualStatus,
        [object]$ResponseBody = $null
    )
    if ($ExpectedStatus -eq $ActualStatus) {
        Write-Host " [PASS] $TestName (Status: $ActualStatus)" -ForegroundColor Green
        $global:passedCount++
        return $true
    } else {
        Write-Host " [FAIL] $TestName (Expected: $ExpectedStatus, Got: $ActualStatus)" -ForegroundColor Red
        if ($ResponseBody) {
            Write-Host "        Body: $($ResponseBody | ConvertTo-Json -Compress)" -ForegroundColor Red
        }
        $global:failedCount++
        return $false
    }
}

function Invoke-Api {
    param(
        [string]$Method,
        [string]$Endpoint,
        [object]$Body = $null,
        [string]$Token = $null
    )
    $headers = @{ "Content-Type" = "application/json" }
    if ($Token) {
        $headers["Authorization"] = "Bearer $Token"
    }

    $params = @{
        Uri = "$baseUrl$Endpoint"
        Method = $Method
        Headers = $headers
        UseBasicParsing = $true
    }

    if ($Body) {
        $params["Body"] = ($Body | ConvertTo-Json -Depth 10)
    }

    try {
        $response = Invoke-WebRequest @params
        $json = if ($response.Content) { $response.Content | ConvertFrom-Json } else { $null }
        return @{ Status = [int]$response.StatusCode; Body = $json }
    } catch {
        $statusCode = if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { 500 }
        $errorBody = $null
        try {
            if ($_.Exception.Response) {
                $reader = [System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
                $errorBody = $reader.ReadToEnd() | ConvertFrom-Json
            }
        } catch {}
        return @{ Status = $statusCode; Body = $errorBody }
    }
}

Write-Host "`n--- 1. SEGURIDAD Y JWT ---" -ForegroundColor Yellow
$unauth = Invoke-Api -Method "GET" -Endpoint "/api/v1/institutions"
Assert-Result "Acceso sin token debe retornar 401" 401 $unauth.Status $unauth.Body

$badToken = Invoke-Api -Method "GET" -Endpoint "/api/v1/institutions" -Token "token_invalido_123"
Assert-Result "Acceso con token inválido debe retornar 401" 401 $badToken.Status $badToken.Body

Write-Host "`n--- 2. AUTENTICACIÓN (ms-auth) ---" -ForegroundColor Yellow
$regBody = @{
    name = "Admin Master"
    email = "admin.master@gems.com"
    password = "SecurePass123!"
    role = "ADMIN"
    institutionId = "inst-gems-01"
}
$regRes = Invoke-Api -Method "POST" -Endpoint "/api/v1/auth/register" -Body $regBody
Assert-Result "Registrar usuario admin" 201 $regRes.Status $regRes.Body

$loginBody = @{
    email = "admin.master@gems.com"
    password = "SecurePass123!"
}
$loginRes = Invoke-Api -Method "POST" -Endpoint "/api/v1/auth/login" -Body $loginBody
Assert-Result "Login de usuario admin" 200 $loginRes.Status $loginRes.Body

$jwtToken = if ($loginRes.Body -and $loginRes.Body.token) { $loginRes.Body.token } else { "" }
if (-not $jwtToken) {
    Write-Host "CRÍTICO: No se pudo obtener el token JWT para continuar las pruebas." -ForegroundColor Red
    exit 1
}

Write-Host "`n--- 3. INSTITUCIONES Y BRANDING (ms-admin) ---" -ForegroundColor Yellow
$instBody = @{
    id = "inst-gems-01"
    name = "Universidad GEMS Global"
    type = "UNIVERSITY"
    status = "ACTIVE"
    metadata = @{
        description = "Sede Principal GEMS"
        website = "https://gems.edu"
        contactEmail = "contact@gems.edu"
        phoneNumber = "+57 300 000 0000"
        address = "Calle 100 #10-20"
        subscriptionType = "PREMIUM"
        maxUsers = 500
    }
}
$createInst = Invoke-Api -Method "POST" -Endpoint "/api/v1/institutions" -Body $instBody -Token $jwtToken
Assert-Result "Crear institución" 201 $createInst.Status $createInst.Body

$getAllInst = Invoke-Api -Method "GET" -Endpoint "/api/v1/institutions" -Token $jwtToken
Assert-Result "Listar todas las instituciones" 200 $getAllInst.Status $getAllInst.Body

$getInst = Invoke-Api -Method "GET" -Endpoint "/api/v1/institutions/inst-gems-01" -Token $jwtToken
Assert-Result "Obtener institución por ID" 200 $getInst.Status $getInst.Body

$instBody.name = "Universidad GEMS Global Actualizada"
$updateInst = Invoke-Api -Method "PUT" -Endpoint "/api/v1/institutions/inst-gems-01" -Body $instBody -Token $jwtToken
Assert-Result "Actualizar institución" 200 $updateInst.Status $updateInst.Body

$brandingBody = @{
    companyId = "inst-gems-01"
    domain = "gems.edu"
    primaryColor = "#1E40AF"
    secondaryColor = "#3B82F6"
    theme = "light"
}
$createBrand = Invoke-Api -Method "POST" -Endpoint "/api/v1/branding" -Body $brandingBody -Token $jwtToken
Assert-Result "Crear branding" 201 $createBrand.Status $createBrand.Body

$getBrand = Invoke-Api -Method "GET" -Endpoint "/api/v1/branding/inst-gems-01" -Token $jwtToken
Assert-Result "Obtener branding por companyId" 200 $getBrand.Status $getBrand.Body

$brandingBody.primaryColor = "#111827"
$updateBrand = Invoke-Api -Method "PUT" -Endpoint "/api/v1/branding/inst-gems-01" -Body $brandingBody -Token $jwtToken
Assert-Result "Actualizar branding" 200 $updateBrand.Status $updateBrand.Body

Write-Host "`n--- 4. USUARIOS (ms-auth) ---" -ForegroundColor Yellow
$getUsers = Invoke-Api -Method "GET" -Endpoint "/api/v1/users" -Token $jwtToken
Assert-Result "Listar todos los usuarios" 200 $getUsers.Status $getUsers.Body

$adminUserId = if ($loginRes.Body -and $loginRes.Body.userId) { $loginRes.Body.userId } else { 1 }
$getUserById = Invoke-Api -Method "GET" -Endpoint "/api/v1/users/$adminUserId" -Token $jwtToken
Assert-Result "Obtener usuario por ID" 200 $getUserById.Status $getUserById.Body

$getUsersInst = Invoke-Api -Method "GET" -Endpoint "/api/v1/users/institution/inst-gems-01" -Token $jwtToken
Assert-Result "Obtener usuarios por institución" 200 $getUsersInst.Status $getUsersInst.Body

$updateUserBody = @{
    name = "Admin Master Editado"
    role = "ADMIN"
    institutionId = "inst-gems-01"
}
$updateUser = Invoke-Api -Method "PUT" -Endpoint "/api/v1/users/$adminUserId" -Body $updateUserBody -Token $jwtToken
Assert-Result "Actualizar usuario" 200 $updateUser.Status $updateUser.Body

Write-Host "`n--- 5. ESTUDIANTES (ms-education) ---" -ForegroundColor Yellow
$st1Body = @{
    name = "Carlos Mario"
    email = "carlos.mario@gems.com"
    birthDate = "2000-01-15"
    country = "Colombia"
    city = "Bogotá"
    documentType = "CC"
    documentNumber = "1010101010"
}
$st1Res = Invoke-Api -Method "POST" -Endpoint "/api/v1/students/register" -Body $st1Body -Token $jwtToken
Assert-Result "Registrar Estudiante 1" 201 $st1Res.Status $st1Res.Body
$studentId1 = if ($st1Res.Body) { $st1Res.Body.id } else { 1 }

$st2Body = @{
    name = "Ana Sofía"
    email = "ana.sofia@gems.com"
    birthDate = "2002-08-20"
    country = "Colombia"
    city = "Medellín"
    documentType = "CC"
    documentNumber = "2020202020"
}
$st2Res = Invoke-Api -Method "POST" -Endpoint "/api/v1/students/register" -Body $st2Body -Token $jwtToken
Assert-Result "Registrar Estudiante 2" 201 $st2Res.Status $st2Res.Body
$studentId2 = if ($st2Res.Body) { $st2Res.Body.id } else { 2 }

$getStudents = Invoke-Api -Method "GET" -Endpoint "/api/v1/students" -Token $jwtToken
Assert-Result "Listar todos los estudiantes" 200 $getStudents.Status $getStudents.Body

$getStudent1 = Invoke-Api -Method "GET" -Endpoint "/api/v1/students/$studentId1" -Token $jwtToken
Assert-Result "Obtener Estudiante 1 por ID" 200 $getStudent1.Status $getStudent1.Body

$st1Body.name = "Carlos Mario Actualizado"
$updateSt1 = Invoke-Api -Method "PUT" -Endpoint "/api/v1/students/$studentId1" -Body $st1Body -Token $jwtToken
Assert-Result "Actualizar Estudiante 1" 200 $updateSt1.Status $updateSt1.Body

Write-Host "`n--- 6. CURSOS (ms-education) ---" -ForegroundColor Yellow
$courseBody = @{
    title = "Java Reactivo con WebFlux"
    description = "Curso avanzado de microservicios reactivos"
    status = "PUBLISHED"
    institutionId = "inst-gems-01"
    modules = @(
        @{
            title = "Módulo 1: Introducción"
            orderIndex = 1
            lessons = @(
                @{
                    title = "Lección 1.1: WebFlux y R2DBC"
                    orderIndex = 1
                    contents = @(
                        @{
                            type = "VIDEO"
                            value = "https://youtube.com/watch?v=demo"
                            orderIndex = 1
                        }
                    )
                }
            )
        }
    )
}
$createCourse = Invoke-Api -Method "POST" -Endpoint "/api/v1/courses" -Body $courseBody -Token $jwtToken
Assert-Result "Crear Curso con módulos/lecciones" 201 $createCourse.Status $createCourse.Body
$courseId = if ($createCourse.Body) { $createCourse.Body.id } else { 1 }

$getAllCourses = Invoke-Api -Method "GET" -Endpoint "/api/v1/courses" -Token $jwtToken
Assert-Result "Listar todos los cursos" 200 $getAllCourses.Status $getAllCourses.Body

$getCourseById = Invoke-Api -Method "GET" -Endpoint "/api/v1/courses/$courseId" -Token $jwtToken
Assert-Result "Obtener curso por ID" 200 $getCourseById.Status $getCourseById.Body

$getCoursesInst = Invoke-Api -Method "GET" -Endpoint "/api/v1/courses/institution/inst-gems-01" -Token $jwtToken
Assert-Result "Obtener cursos por institución" 200 $getCoursesInst.Status $getCoursesInst.Body

$courseBody.title = "Java Reactivo Master Class"
$updateCourse = Invoke-Api -Method "PUT" -Endpoint "/api/v1/courses/$courseId" -Body $courseBody -Token $jwtToken
Assert-Result "Actualizar curso" 200 $updateCourse.Status $updateCourse.Body

Write-Host "`n--- 7. QUIZZES (ms-education) ---" -ForegroundColor Yellow
$lessonId = if ($createCourse.Body -and $createCourse.Body.modules -and $createCourse.Body.modules[0].lessons) { $createCourse.Body.modules[0].lessons[0].id } else { 1 }
$quizBody = @{
    lessonId = $lessonId
    title = "Quiz: Evaluación WebFlux"
    passingScore = 70
    questions = @(
        @{
            text = "¿Qué es WebFlux?"
            options = @("Framework reactivo", "Base de datos", "Servidor FTP", "Compilador")
            correctOption = "Framework reactivo"
        },
        @{
            text = "¿Qué retorna un Mono?"
            options = @("0 o 1 elemento", "N elementos", "Texto plano", "Objeto nulo")
            correctOption = "0 o 1 elemento"
        }
    )
}
$createQuiz = Invoke-Api -Method "POST" -Endpoint "/api/v1/quizzes" -Body $quizBody -Token $jwtToken
Assert-Result "Crear Quiz" 201 $createQuiz.Status $createQuiz.Body
$quizId = if ($createQuiz.Body) { $createQuiz.Body.id } else { 1 }

$getQuizById = Invoke-Api -Method "GET" -Endpoint "/api/v1/quizzes/$quizId" -Token $jwtToken
Assert-Result "Obtener Quiz por ID" 200 $getQuizById.Status $getQuizById.Body

$getQuizByLesson = Invoke-Api -Method "GET" -Endpoint "/api/v1/quizzes/lesson/$lessonId" -Token $jwtToken
Assert-Result "Obtener Quiz por Lesson ID" 200 $getQuizByLesson.Status $getQuizByLesson.Body

$quizBody.title = "Quiz: Evaluación WebFlux Avanzada"
$updateQuiz = Invoke-Api -Method "PUT" -Endpoint "/api/v1/quizzes/$quizId" -Body $quizBody -Token $jwtToken
Assert-Result "Actualizar Quiz" 200 $updateQuiz.Status $updateQuiz.Body

Write-Host "`n--- 8. RUTAS DE APRENDIZAJE (ms-education) ---" -ForegroundColor Yellow
$lpBody = @{
    title = "Ruta Backend Architect"
    description = "Ruta de especialización en Spring Boot"
    institutionId = "inst-gems-01"
    courseIds = @($courseId)
}
$createLp = Invoke-Api -Method "POST" -Endpoint "/api/v1/learning-paths" -Body $lpBody -Token $jwtToken
Assert-Result "Crear Ruta de Aprendizaje" 201 $createLp.Status $createLp.Body
$lpId = if ($createLp.Body) { $createLp.Body.id } else { 1 }

$getAllLps = Invoke-Api -Method "GET" -Endpoint "/api/v1/learning-paths" -Token $jwtToken
Assert-Result "Listar todas las rutas de aprendizaje" 200 $getAllLps.Status $getAllLps.Body

$getLpById = Invoke-Api -Method "GET" -Endpoint "/api/v1/learning-paths/$lpId" -Token $jwtToken
Assert-Result "Obtener Ruta de Aprendizaje por ID" 200 $getLpById.Status $getLpById.Body

$getLpsInst = Invoke-Api -Method "GET" -Endpoint "/api/v1/learning-paths/institution/inst-gems-01" -Token $jwtToken
Assert-Result "Obtener Rutas por institución" 200 $getLpsInst.Status $getLpsInst.Body

$lpBody.title = "Ruta Backend Expert 2026"
$updateLp = Invoke-Api -Method "PUT" -Endpoint "/api/v1/learning-paths/$lpId" -Body $lpBody -Token $jwtToken
Assert-Result "Actualizar Ruta de Aprendizaje" 200 $updateLp.Status $updateLp.Body

Write-Host "`n--- 9. INSCRIPCIONES (ms-education) ---" -ForegroundColor Yellow
$enrollBody = @{
    studentId = $studentId1
    courseId = $courseId
}
$createEnroll = Invoke-Api -Method "POST" -Endpoint "/api/v1/enrollments" -Body $enrollBody -Token $jwtToken
Assert-Result "Inscribir estudiante 1" 201 $createEnroll.Status $createEnroll.Body
$enrollmentId = if ($createEnroll.Body) { $createEnroll.Body.id } else { 1 }

$bulkEnrollBody = @{
    studentIds = @($studentId2)
    courseId = $courseId
}
$bulkEnroll = Invoke-Api -Method "POST" -Endpoint "/api/v1/enrollments/bulk" -Body $bulkEnrollBody -Token $jwtToken
Assert-Result "Inscripción masiva de estudiante 2" 201 $bulkEnroll.Status $bulkEnroll.Body

$getEnrollById = Invoke-Api -Method "GET" -Endpoint "/api/v1/enrollments/$enrollmentId" -Token $jwtToken
Assert-Result "Obtener inscripción por ID" 200 $getEnrollById.Status $getEnrollById.Body

$getEnrollStudent = Invoke-Api -Method "GET" -Endpoint "/api/v1/enrollments/student/$studentId1" -Token $jwtToken
Assert-Result "Obtener inscripciones de estudiante 1" 200 $getEnrollStudent.Status $getEnrollStudent.Body

$getEnrollCourse = Invoke-Api -Method "GET" -Endpoint "/api/v1/enrollments/course/$courseId" -Token $jwtToken
Assert-Result "Obtener inscripciones de curso" 200 $getEnrollCourse.Status $getEnrollCourse.Body

$updateProgress = Invoke-Api -Method "PUT" -Endpoint "/api/v1/enrollments/$enrollmentId/progress?progress=100" -Token $jwtToken
Assert-Result "Actualizar progreso de inscripción a 100%" 200 $updateProgress.Status $updateProgress.Body

Write-Host "`n--- 10. EVALUACIÓN DE QUIZ (CALIFICACIÓN) ---" -ForegroundColor Yellow
$q1Id = if ($createQuiz.Body -and $createQuiz.Body.questions -and $createQuiz.Body.questions[0]) { $createQuiz.Body.questions[0].id } else { 1 }
$q2Id = if ($createQuiz.Body -and $createQuiz.Body.questions -and $createQuiz.Body.questions[1]) { $createQuiz.Body.questions[1].id } else { 2 }

$submitBody = @{
    studentId = $studentId1
    answers = @(
        @{ questionId = $q1Id; selectedOption = "Framework reactivo" },
        @{ questionId = $q2Id; selectedOption = "0 o 1 elemento" }
    )
}
$submitRes = Invoke-Api -Method "POST" -Endpoint "/api/v1/quizzes/$quizId/submit" -Body $submitBody -Token $jwtToken
Assert-Result "Responder Quiz con 100% de aciertos" 200 $submitRes.Status $submitRes.Body
if ($submitRes.Body) {
    Write-Host "        Puntaje: $($submitRes.Body.score)%, Aprobado: $($submitRes.Body.passed)" -ForegroundColor Cyan
}

Write-Host "`n--- 11. PRUEBAS DE CASOS DE ERROR (VALIDACIONES) ---" -ForegroundColor Yellow
$dupReg = Invoke-Api -Method "POST" -Endpoint "/api/v1/auth/register" -Body $regBody
Assert-Result "Registrar email duplicado debe retornar 409 Conflict" 409 $dupReg.Status $dupReg.Body

$badLogin = Invoke-Api -Method "POST" -Endpoint "/api/v1/auth/login" -Body @{ email = "admin.master@gems.com"; password = "WrongPassword!" }
Assert-Result "Login con clave errónea debe retornar 401 Unauthorized" 401 $badLogin.Status $badLogin.Body

$badCourse = Invoke-Api -Method "POST" -Endpoint "/api/v1/courses" -Body @{ description = "Sin título" } -Token $jwtToken
Assert-Result "Crear curso sin título debe retornar 400 Bad Request" 400 $badCourse.Status $badCourse.Body

$notFoundCourse = Invoke-Api -Method "GET" -Endpoint "/api/v1/courses/999999" -Token $jwtToken
Assert-Result "Consultar recurso inexistente debe retornar 404 Not Found" 404 $notFoundCourse.Status $notFoundCourse.Body

Write-Host "`n--- 12. ELIMINACIONES (DELETE) ---" -ForegroundColor Yellow
$delBrand = Invoke-Api -Method "DELETE" -Endpoint "/api/v1/branding/inst-gems-01" -Token $jwtToken
Assert-Result "Eliminar branding" 204 $delBrand.Status $delBrand.Body

$delEnroll = Invoke-Api -Method "DELETE" -Endpoint "/api/v1/enrollments/$enrollmentId" -Token $jwtToken
Assert-Result "Eliminar inscripción" 204 $delEnroll.Status $delEnroll.Body

$delQuiz = Invoke-Api -Method "DELETE" -Endpoint "/api/v1/quizzes/$quizId" -Token $jwtToken
Assert-Result "Eliminar quiz" 204 $delQuiz.Status $delQuiz.Body

$delLp = Invoke-Api -Method "DELETE" -Endpoint "/api/v1/learning-paths/$lpId" -Token $jwtToken
Assert-Result "Eliminar ruta de aprendizaje" 204 $delLp.Status $delLp.Body

$delCourse = Invoke-Api -Method "DELETE" -Endpoint "/api/v1/courses/$courseId" -Token $jwtToken
Assert-Result "Eliminar curso" 204 $delCourse.Status $delCourse.Body

$delStudent = Invoke-Api -Method "DELETE" -Endpoint "/api/v1/students/$studentId1" -Token $jwtToken
Assert-Result "Eliminar estudiante 1" 204 $delStudent.Status $delStudent.Body

$disableUser = Invoke-Api -Method "DELETE" -Endpoint "/api/v1/users/$adminUserId" -Token $jwtToken
Assert-Result "Desactivar usuario admin" 204 $disableUser.Status $disableUser.Body

$delInst = Invoke-Api -Method "DELETE" -Endpoint "/api/v1/institutions/inst-gems-01" -Token $jwtToken
Assert-Result "Eliminar institución" 204 $delInst.Status $delInst.Body

Write-Host "`n=============================================================" -ForegroundColor Cyan
Write-Host "   RESUMEN FINAL DE PRUEBAS:" -ForegroundColor Cyan
Write-Host "   PASADAS: $global:passedCount" -ForegroundColor Green
Write-Host "   FALLADAS: $global:failedCount" -ForegroundColor $(if ($global:failedCount -eq 0) { "Green" } else { "Red" })
Write-Host "=============================================================" -ForegroundColor Cyan

if ($global:failedCount -gt 0) {
    exit 1
} else {
    exit 0
}
