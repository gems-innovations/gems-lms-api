# Integración gems-lms-web ↔ gems-lms-api

Estado a 2026-10-02 (ramas `feature/integracion-front` en este repo y `feature/integracion-back` en gems-lms-web).

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
| Gestión de usuarios | `GET /users/institution/{id}`, `POST /auth/register`, `PATCH /users/{id}/status`, `DELETE /users/{id}` | Alta sin contraseña → el back devuelve una temporal y se muestra al admin |
| Instituciones | `GET/POST/PUT/DELETE /institutions` | PUT exige `name` y `type`; el front los completa desde el registro actual |
| Cursos y editor | `GET/POST/PUT/DELETE /courses` | Añadir módulo/lección/contenido = leer curso + PUT del árbol completo |
| Rutas de aprendizaje | `/learning-paths` | |
| Inscripciones y progreso | `/enrollments`, `/enrollments/bulk`, `PUT /enrollments/{id}/progress` | `studentId` = id de usuario de ms-auth |
| Panel docente / matrículas | los anteriores | |

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

Pendiente:

1. `jwt.secret` y contraseñas tienen valores por defecto en `application.properties`: si falta la
   variable de entorno, arranca con un secreto conocido.
2. El staff puede inscribir cualquier `studentId`: ms-education no puede verificar en ms-auth que el
   usuario pertenezca a la institución.

### Calidad

3. **Cobertura**: `jacocoTestCoverageVerification` exige 100% y hoy hay ~60% (admin), ~70% (auth) y
   ~40% (education), por eso `./gradlew build` solo pasa con `-x jacocoTestCoverageVerification` (como
   indica el README). Hay que subir la cobertura o fijar una meta alcanzable. Los tests en sí (306)
   compilan y pasan: antes 76 errores de compilación impedían correr ms-admin y ms-education.
4. La capa `application` de ms-education importa excepciones de `infrastructure.driving.rest`
   (rompe la arquitectura limpia que pide el README).
5. `/actuator/health` responde 401 (no está en las rutas públicas); el README dice lo contrario.
6. README desactualizado (puertos, gateway, compose local); scripts de arranque no portables a Windows.

### Funcionalidad que el front ya tiene y el back no

7. **Grupos/cohortes**: sin API. Hoy se guardan en el navegador (por institución) y el instructor ve
    un grupo automático "Todos los inscritos" por curso.
8. **Archivos**: no hay almacenamiento para entregas de tareas, miniaturas ni logos (solo URLs).
9. **Rutas de aprendizaje**: faltan estado (borrador/publicada), etiquetas, miniatura, pasos
    opcionales y **inscripción a rutas** (hoy en el navegador).
10. **Encuestas de curso, notificaciones y reseñas**: sin API (encuestas y notificaciones son locales;
    reseñas quedan vacías).
11. Lecciones y módulos no guardan `description`/`isFree`; los bloques no tienen fecha de entrega.
12. `usersCount` de instituciones nunca se sincroniza con ms-auth; `completionRate`, `averageRating`
    nunca se calculan; `enrolledCount` no baja al borrar una inscripción.
13. `DELETE /users/{id}` solo desactiva (el usuario reaparece como inactivo al recargar).
14. Sin flujos de **recuperar contraseña**, **auto-registro** ni **cambio de contraseña temporal**.
15. Sin paginación en usuarios, rutas e inscripciones; la tabla `students` de ms-education quedó sin uso.

## Qué falta — front

1. "¿Olvidaste tu contraseña?" y "Regístrate" son enlaces muertos (`href="#"`).
2. El proyecto no tiene ni un test (`*.spec.ts`: 0).
3. Las librerías se compilan con `environment.ts` (localhost:8080): para producción hay que definir
   `globalThis.API_BASE_URL` antes de arrancar la app o compilar las librerías con el entorno de prod.
4. Corregido de paso: el build de producción fallaba porque `instructor/**` no estaba declarado como
   renderizado en cliente en `app.routes.server.ts`.
