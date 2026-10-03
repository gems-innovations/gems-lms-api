# Integración gems-lms-web ↔ gems-lms-api

Estado a 2026-10-03 (ramas `feature/integracion-front` en este repo y `feature/integracion-back` en gems-lms-web).

## Cómo levantar todo en local

Requisitos: Docker Desktop (con WSL 2 en Windows), Java 24, Node 24 y el `.env` en la raíz de este repo.

```bash
# Back: PostgreSQL + Redis, ms-auth, ms-admin, ms-education y api-gateway
./dev-up.sh --seed        # --seed carga usuarios, instituciones y el catálogo de demo

# Front (en gems-lms-web)
npm install
npm start                 # compila las librerías y sirve en http://localhost:4200
```

El front habla **solo con el api-gateway** (`http://localhost:8080/api/v1`). Los microservicios
quedan en 8081 (auth), 8082 (admin) y 8083 (education), cada uno con su Swagger en
`/swagger-ui.html`.

Usuarios de desarrollo (contraseña: `DEV_PASSWORD` en `seed-dev.sh`):

| Correo | Rol | Institución |
|---|---|---|
| super@gems.lms | SUPER_ADMIN (lo crea ms-auth al arrancar) | — |
| admin@unal.edu.co | ADMIN | inst-1 |
| admin@pragma.co | ADMIN | inst-2 |
| instructor@unal.edu.co | INSTRUCTOR | inst-1 |
| estudiante@unal.edu.co | STUDENT | inst-1 |

### Trampas conocidas

- **Git Bash en Windows**: MSYS convierte variables que parecen rutas (`AUTH_LOGIN_PATH=/api/v1/auth/login`)
  en rutas de Windows al lanzar Java, y el login responde 401 vacío. `dev-up.sh` exporta
  `MSYS_NO_PATHCONV=1` y `MSYS2_ENV_CONV_EXCL='*'`. `start-microservices.sh` no lo hace.
- **Tildes con curl en Git Bash**: los argumentos se recodifican y el back responde 500 por JSON inválido.
  Enviar el cuerpo por stdin (`--data-binary @-`), como hace `seed-dev.sh`.
- Para desarrollo local usar `docker-compose-local.yml`; `docker-compose.yml` es el de producción
  (imágenes de Docker Hub + nginx con certificados).
- Puertos reales de PostgreSQL: admin 5432, auth 5433, education 5434 (el README los tenía invertidos).
- `start-microservices.ps1` tiene fija la ruta del JDK de otra máquina (`C:\Users\Lu\...`).

## Qué se conectó

| Área del front | Endpoints | Notas |
|---|---|---|
| Login / sesión | `POST /auth/login` | JWT guardado en la sesión; interceptor añade `Authorization` y cierra sesión ante 401 |
| Branding | `GET /institutions/{id}` | Se carga al iniciar sesión y se guarda en la sesión |
| Gestión de usuarios | `GET /users/institution/{id}`, `POST /auth/register`, `PUT /users/{id}/status`, `DELETE /users/{id}` | Alta sin contraseña → el back devuelve una temporal y se muestra al admin |
| Instituciones | `GET/POST/PUT/DELETE /institutions` | PUT exige `name` y `type`; el front los completa desde el registro actual |
| Cursos y editor | `GET/POST/PUT/DELETE /courses` | Añadir módulo/lección/contenido = leer curso + PUT del árbol completo |
| Rutas de aprendizaje | `/learning-paths` | |
| Inscripciones y progreso | `/enrollments`, `/enrollments/bulk`, `PUT /enrollments/{id}/progress` | `studentId` = id de usuario de ms-auth |
| Panel docente / matrículas | los anteriores | |
| Auditoría administrativa | `GET /audit/events` | Historial filtrable y paginado; cada admin ve solo su institución |

Los bloques de contenido del front (video, documento, quiz, tarea…) se guardan en `contents.value`
como JSON con todos sus campos; `contents.type` lleva el tipo.

## Cambios hechos en el back

- **Bug grave corregido**: actualizar un curso borraba y recreaba todos sus módulos, lecciones y
  contenidos (incluso al cambiar solo el título, porque `findById` carga el árbol). Los ids cambiaban
  y, por el `ON DELETE CASCADE`, **se borraban todos los quizzes del curso**. Ahora el guardado
  sincroniza el árbol: actualiza lo que trae `id`, inserta lo nuevo y borra solo lo que se quitó.
  Módulos, lecciones y contenidos aceptan `id` opcional en el request.
- Borrar una institución dejaba su branding huérfano; recrear una institución con el mismo id daba 500
  (índice único de `company_id`) y la dejaba guardada a medias. Ahora se borra el branding con la
  institución y la creación reutiliza un branding existente.
- `ms-education` no tenía la dependencia de springdoc: su Swagger daba 500. Agregada.
- El manejador genérico de errores de `ms-education` convertía 404/405 en 500 y no registraba nada.
  Ahora respeta `ResponseStatusException` y registra la excepción.
- `GET /courses` acepta `institutionId` y mantiene el orden "más recientes primero" (antes `flatMap`
  lo desordenaba).
- Inscripciones: nueva columna `progress_data` (JSON) y `PUT /enrollments/{id}/progress` acepta
  `{ progress, progressData }` en el cuerpo (sigue aceptando `?progress=`).
- `seed-dev.sh`, `seed/demo-courses.json` (11 cursos que antes eran mock del front) y `dev-up.sh`.

## Qué falta — back

### Seguridad

Resuelto (rama `feature/integracion-front`):

- **Autorización por rol e institución** en todos los servicios. El JWT lleva `role` e `institutionId`;
  `shared` expone `AuthenticatedUser`/`CurrentUser` y un acceso denegado responde 403.
  - ms-auth: solo el super admin lista todos los usuarios; los admins gestionan los usuarios de su
    institución (sin tocar super admins ni dar ese rol); cada usuario edita su propio perfil.
  - ms-admin: solo el super admin crea/borra instituciones o cambia su estado; el admin edita la suya;
    el resto solo lee la propia.
  - ms-education: todo queda dentro de la institución del usuario; el staff (admin, instructor) gestiona
    cursos, rutas, quizzes e inscripciones; el estudiante ve cursos publicados y solo se inscribe a sí
    mismo y toca su propio progreso.
- **`POST /auth/register` ya no es público.** El primer super admin se crea al arrancar ms-auth con
  `BOOTSTRAP_SUPERADMIN_EMAIL` y `BOOTSTRAP_SUPERADMIN_PASSWORD` (`dev-up.sh` los define en local).
- Las sesiones del front con tokens anteriores (sin `institutionId`) se descartan y piden login.
- **Quizzes calificados en el servidor y respuestas ocultas.** Intentos (`POST /courses/{id}/blocks/{blockId}/attempts`)
  y entregas (`PUT .../submission`) tienen tablas propias (`quiz_attempts`, `assignment_submissions`);
  el back respeta `maxAttempts` y `passingScore`, y el staff califica con `PUT /submissions/{id}/grade`.
  A los estudiantes se les sirven cursos, rutas y quizzes sin `correctAnswers`, `correctAnswer`,
  `sampleAnswer` ni `explanation` (la explicación llega como retroalimentación del intento).

Resuelto también:

1. **Sin secretos por defecto**: `JWT_SECRET` y `REDIS_PASSWORD` son obligatorios; `JwtSecretGuard` impide arrancar
   con un secreto vacío, de menos de 64 bytes (HS512) o con el valor público que traía el proyecto.
2. **Inscripciones solo dentro de la institución**: al inscribir (cursos, rutas) o armar grupos, ms-education
   pregunta a ms-auth, con el token del usuario, si las personas pertenecen a la institución (403 si no;
   503 si ms-auth no responde). `AUTH_SERVICE_URL` apunta a ms-auth (por defecto localhost:8081).

### Calidad

3. **Cobertura**: el gate de jacoco pasó de 100% (inalcanzable) a **50% de líneas** configurable
   (`coverageMinimum` en `gradle.properties`). Línea base: auth 71%, admin 73%, education 52%.
   Subirlo a medida que crezca la cobertura. `./gradlew test` ya no genera el reporte de cobertura.
4. ~~Capas~~ **Resuelto**: las excepciones pasaron a `application.exceptions`; `application`/`domain` ya no importan
   `infrastructure` en ningún servicio.
5. ~~Health~~ **Resuelto**: faltaba `spring-boot-starter-actuator`; `/actuator/health` responde `UP` sin token en
   los cuatro servicios.
6. ~~README~~ **Resuelto**: README actualizado (gateway, `dev-up.sh`, variables nuevas, health, Git Bash en Windows).

### Funcionalidad que el front ya tiene y el back no

7. ~~**Grupos/cohortes**~~ **Resuelto**: API `/api/v1/groups` (GET/POST/PUT parcial/DELETE, solo staff de
    la institución; cursos y rutas del grupo deben ser de la misma institución). El front migra una vez
    los grupos que tenía en el navegador. Sigue el grupo automático "Todos los inscritos" por curso.
8. ~~**Archivos**~~ **Resuelto**: `POST /files` (multipart, 10 MB) guarda en disco (`FILES_DIR`, `LocalFileStorage`;
    para S3 basta otra implementación de `FileStorage`). Imágenes públicas (`/files/public/{id}`: miniaturas de
    cursos/rutas y logos, con botón de subida en los editores) y archivos privados (entregas de tareas: solo el
    autor y el staff de su institución).
9. ~~**Rutas de aprendizaje**~~ **Resuelto**: estado (borrador/publicada/archivada; el estudiante solo ve
    publicadas), etiquetas, miniatura (URL), pasos opcionales con puntaje mínimo y contador de inscritos.
    Inscripción a rutas en `/learning-paths/{id}/enrollments` y `/learning-paths/enrollments/me`. El avance
    (cursos completados, curso actual, % de obligatorios) lo calcula el back y la inscripción pasa a `completed`.
10. ~~**Encuestas, notificaciones y reseñas**~~ **Resuelto**: encuesta por curso (`/courses/{id}/survey`, respuestas
    de inscritos), reseñas 1–5 (`/courses/{id}/review(s)`) que alimentan `averageRating`/`ratingCount`, y
    notificaciones del servidor (`/notifications`: entrega → staff, calificación → estudiante) con campana en
    los layouts de estudiante e instructor.
11. ~~Detalles de lecciones~~ **Resuelto**: módulos y lecciones guardan `description`, las lecciones `isFree`, y los
    bloques de tarea llevan `dueDate` (en su JSON) que alimenta el calendario de entregas del estudiante.
12. ~~**Contadores**~~ **Resuelto**: `enrolledCount` y `completionRate` de cursos se recalculan en cada
    inscripción, avance y borrado (y al arrancar); las rutas calculan inscritos y % que completó sus cursos
    obligatorios; `usersCount` lo da ms-auth (`GET /users/counts`) y el front lo combina. `averageRating`
    sale de las reseñas.
13. ~~Borrado de usuarios~~ **Resuelto**: `DELETE /users/{id}` limpia los datos antes de borrar la cuenta
    (desactivar es `PUT /users/{id}/status`). ms-auth llama a `DELETE /students/{id}/learning-data` (matrículas,
    intentos, entregas, encuestas, reseñas, rutas, grupos y notificaciones, en una transacción).
14. ~~**Contraseñas**~~ **Resuelto** (sin correo real): las cuentas creadas con contraseña temporal deben
    cambiarla al entrar (`mustChangePassword`, `POST /auth/change-password`); "olvidé mi contraseña" con
    token de un uso y 1 h (`/auth/forgot-password`, `/auth/reset-password`). El enlace se envía por **SMTP**
    (`MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`; sirve SendGrid/SES por SMTP); sin
    `MAIL_HOST` se escribe en `logs/ms-auth.log`.
15. ~~Paginación~~ **Resuelto**: `GET /users/institution/{id}` (en SQL, con `search`), `/learning-paths` y
    `/enrollments/institution/{id}` aceptan `page`/`limit` y devuelven el total en `X-Total-Count`; sin `page`
    responden la lista completa como antes. La lista de usuarios del front tiene búsqueda y paginación. El registro
    `students` de ms-education queda marcado `@Deprecated` (las cuentas viven en ms-auth).
16. **Auditoría administrativa resuelta**: el gateway registra automáticamente las operaciones autenticadas de
    creación, cambio y eliminación, incluyendo usuario, rol, institución, ruta, método, resultado HTTP, IP, navegador
    y fecha. `GET /audit/events` permite filtrar por texto, acción y fechas, con paginación. Un administrador solo ve
    su institución y el super administrador puede consultar el historial global. Un fallo al guardar la auditoría se
    registra en los logs y no altera la operación original.
17. **Certificados verificables resueltos**: al abrir "Mis certificaciones", el servidor emite de forma idempotente
    una credencial para cada curso o ruta cuya finalización esté confirmada. Conserva el nombre, institución, título,
    docente y fechas como fotografía histórica, asigna un código `GEMS-*` único y permite comprobarlo públicamente en
    `GET /certificates/verify/{code}` o en `/certificates/verify/{code}` del front. Al eliminar los datos de aprendizaje
    de una cuenta también se eliminan sus credenciales.

## Qué falta — front

1. **Resuelto**: recuperación y cambio de contraseña conectados. Las cuentas las crea el administrador
   de la institución; se reemplazó el enlace de registro por esa indicación.
2. **Resuelto**: pruebas de contratos HTTP, sesión, permisos de navegación y contraseñas en shared,
   auth, education y main. `npm run test:ci` ejecuta las suites con ChromeHeadless; para iterar, ejecutar
   solo el proyecto afectado. La cobertura del front aún no es exhaustiva.
3. **Resuelto**: `config.js` se carga antes de la aplicación. El servidor SSR lo genera desde
   `API_BASE_URL`; el despliegue estático permite editarlo sin recompilar. README del front documenta
   compilación, `NG_ALLOWED_HOSTS`, puerto, CORS y configuración del gateway.
4. Corregido de paso: el build de producción fallaba porque `instructor/**` no estaba declarado como
   renderizado en cliente en `app.routes.server.ts`.
5. **Resuelto**: nueva vista **Auditoría** para administradores y super administradores, con filtros por recurso,
   usuario, acción y rango de fechas, estados de carga/error y paginación.
6. **Resuelto**: "Mis certificaciones" usa credenciales persistentes del servidor, conserva la descarga en PDF y
   dispone de una página pública para comprobar código, titular, contenido, institución y fechas.

## Pendientes de revisión y despliegue

- Configurar SMTP real, dominios, HTTPS y almacenamiento persistente de archivos al desplegar.
  El compose incluye el gateway, las variables de correo/bootstrap y un volumen persistente de uploads;
  las imágenes y certificados del entorno real aún necesitan validarse al desplegar.
- **Resuelto**: ms-auth coordina el borrado: primero limpia los datos en ms-education y después elimina
  la cuenta. Si la limpieza falla, conserva la cuenta y responde 503 para reintentar. El front muestra
  ese error. La limpieza es idempotente; no existe una transacción distribuida entre las dos bases.
  Configurar `EDUCATION_SERVICE_URL` en ms-auth (localhost:8083 en local; nombre del servicio en Docker).
- **Resuelto**: la API antigua `/quizzes` comprueba el curso dueño de la lección para leer, crear,
  actualizar y borrar. Para enviar respuestas exige una inscripción; actualizar también valida la lección destino.
- **Resuelto**: `minimumScore` exige completar el curso y alcanzar el promedio de sus evaluaciones:
  mejor intento por quiz y calificación vigente por tarea, con el mismo peso por bloque. Una evaluación
  pendiente o un curso sin evaluaciones no satisface un umbral positivo. El rango permitido es 0–100.
- **Resuelto**: cada petición autenticada valida la cuenta vigente en ms-auth. Desactivar, borrar,
  cambiar permisos o contraseña invalida la sesión anterior. Las contraseñas temporales bloquean
  operaciones de aplicación también en el back; permiten consultar la sesión y cambiar la contraseña.
  El front renueva el token tras cambiarla. Los tokens antiguos requieren iniciar sesión de nuevo.
  `JWT_EXPIRATION` se interpreta en milisegundos (3600000 = 1 h), como indica la configuración predeterminada.
  Todos los servicios necesitan alcanzar ms-auth mediante `AUTH_SERVICE_URL`; si no está disponible,
  el acceso protegido falla con 503, sin permitir sesiones que no puedan verificarse.

Estos puntos requieren verificación antes de afirmar que todas las brechas están cerradas.

Validación del cierre local: build completo del back sin fallos y cobertura mínima al 50%;
31 pruebas del front (shared 2, auth 14, education 10, admin 1, main 4) y build de producción. Smoke en servicios
activos: contraseñas temporales, renovación/revocación, aislamiento de quizzes y auditoría por institución. Compose validado;
imágenes Docker y despliegue remoto aún no ejecutados.

### Verificación local reproducible

`./gradlew build -x bootJar` comprueba tests y cobertura; para iterar usar `./gradlew :ms-education:test`
o el módulo afectado. `smoke-security.ps1` (PowerShell 7, con datos demo y servicios activos) verifica
contraseña temporal, cambio, revocación al desactivar/reactivar/borrar y aislamiento de quizzes.
Crea una cuenta y un quiz temporales, y los elimina al terminar. No imprime contraseñas ni tokens.
