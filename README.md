# GEMS LMS API

Sistema de gestión de aprendizaje (LMS) basado en arquitectura de microservicios con Spring Boot, WebFlux y PostgreSQL.

## 🏗️ Arquitectura

### Componentes Principales

El proyecto sigue una **arquitectura de microservicios** basada en **Clean Architecture + DDD (Domain-Driven Design)**:

- **api-gateway** (puerto **8080**): punto de entrada único para el front (`http://localhost:8080/api/v1`).
  Enruta a cada microservicio, aplica CORS, valida el JWT y registra las operaciones autenticadas en la auditoría.
- **Microservicios**:
  - `ms-auth` (8081): autenticación, usuarios, contraseñas (cambio, recuperación por correo) y auditoría
  - `ms-admin` (8082): instituciones y branding
  - `ms-education` (8083): cursos, rutas, inscripciones, quizzes y entregas calificados en el servidor,
    grupos, encuestas, reseñas, notificaciones y archivos
- **Bases de Datos**: una base PostgreSQL independiente por microservicio
- **Redis**: rate limiting
- **Shared Module**: seguridad (JWT, autorización por rol e institución), filtros y utilidades comunes

### Estructura de Microservicios

Cada microservicio sigue la estructura de Clean Architecture:

```
ms-[nombre]/
├── domain/              # Capa de dominio (entidades, value objects)
├── application/         # Capa de aplicación (use cases, gateways, commands, excepciones)
└── infrastructure/      # Capa de infraestructura (repositorios, controladores, config)
```

`application` y `domain` no dependen de `infrastructure`.

## 📊 Flujo de Información

```
Front → api-gateway (8080) → ms-auth | ms-admin | ms-education
```

### Filtros Aplicados (en orden)

1. **SecurityHeadersFilter**: agrega headers de seguridad HTTP
2. **RateLimitFilter**: limita las peticiones por cliente usando Redis
3. **JwtAuthenticationFilter**: valida el token JWT del header `Authorization`
   - Rutas públicas: login, `forgot-password`, `reset-password`, `/actuator/health`, Swagger y las imágenes
     públicas (`/api/v1/files/public/**`)
   - Si el token no es válido, retorna `401 Unauthorized`

### Procesamiento de la Petición

1. **Controlador REST**: comprueba permisos (rol e institución del JWT) y usa los **Use Cases**
2. **Use Case**: ejecuta la lógica de negocio a través de **Gateways** (interfaces)
3. **Repositorio Adapter**: implementa los gateways con R2DBC (programación reactiva)

## 🚀 Guía de Ejecución

### Atajo para desarrollo local

```bash
./dev-up.sh --seed
```

Levanta las bases y Redis (`docker-compose-local.yml`), los cuatro servicios en segundo plano (logs en
`logs/ms-*.log`) y carga datos de prueba (usuarios, instituciones y cursos). Ver
[docs/integracion-front-back.md](docs/integracion-front-back.md) para la integración con gems-lms-web y las
trampas de Windows.

### Prerrequisitos

- Java 24
- Docker y Docker Compose
- Bash: Linux/Mac, o **Git Bash** en Windows (los scripts ya evitan la conversión de rutas de MSYS)

### 1. Crear el Archivo .env

Crear un archivo `.env` en la raíz del proyecto:

```env
AUTH_PORT=8081
ADMIN_PORT=8082
EDUCATION_PORT=8083

# Obligatorio, al menos 64 caracteres aleatorios (HS512). Sin él los servicios no arrancan.
JWT_SECRET=genera-un-secreto-aleatorio-de-al-menos-64-caracteres-xxxxxxxxxxxxxxxxxxxx
JWT_EXPIRATION=3600000

AUTH_LOGIN_PATH=/api/v1/auth/login

REDIS_HOST=localhost
REDIS_PORT=6379
# Obligatorio
REDIS_PASSWORD=your-redis-password

RATE_LIMIT_REQUESTS=100
RATE_LIMIT_WINDOW=60
RATE_LIMIT_KEY_PREFIX=rate_limit:

AUTH_DB_NAME=auth_db
AUTH_DB_USER=auth_user
AUTH_DB_PASSWORD=auth_password

ADMIN_DB_NAME=admin_db
ADMIN_DB_USER=admin_user
ADMIN_DB_PASSWORD=admin_password

EDUCATION_DB_NAME=education_db
EDUCATION_DB_USER=education_user
EDUCATION_DB_PASSWORD=education_password

CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:4200
CORS_ALLOWED_METHODS=GET,POST,PUT,DELETE,OPTIONS
CORS_ALLOWED_HEADERS=*
CORS_ALLOW_CREDENTIALS=true
CORS_MAX_AGE=3600

# Primer super admin (se crea al arrancar ms-auth si no existe). dev-up.sh trae valores de desarrollo.
BOOTSTRAP_SUPERADMIN_EMAIL=super@tu-dominio.com
BOOTSTRAP_SUPERADMIN_PASSWORD=UnaClaveSegura1!

# URL del front, para los enlaces de los correos
FRONTEND_URL=http://localhost:4200

# Correo (recuperación de contraseña). Sin MAIL_HOST los enlaces se escriben en logs/ms-auth.log.
MAIL_HOST=
MAIL_PORT=587
MAIL_USERNAME=
MAIL_PASSWORD=
MAIL_STARTTLS=true
MAIL_FROM=no-reply@tu-dominio.com

# Archivos subidos (ms-education): carpeta local y tamaño máximo (bytes)
FILES_DIR=./data/uploads
FILES_MAX_SIZE_BYTES=10485760

# Dónde los servicios consultan a ms-auth (sesiones y pertenencia institucional)
AUTH_SERVICE_URL=http://localhost:8081
# Dónde ms-auth elimina los datos de aprendizaje antes de borrar una cuenta
EDUCATION_SERVICE_URL=http://localhost:8083
```

**Importante**: usar valores propios y seguros en producción. Nunca subir el `.env` al repositorio.

### 2. Levantar las bases de datos y Redis

```bash
docker compose -f docker-compose-local.yml up -d
```

- `postgres-admin` en el puerto **5432**
- `postgres-auth` en el puerto **5433**
- `postgres-education` en el puerto **5434**
- `redis` en el puerto **6379**

Las tablas se crean (y migran) solas al arrancar cada servicio, desde su `schema.sql`.

### 3. Compilar

```bash
./gradlew build -x bootJar
```

### 4. Ejecutar los Microservicios

Con `dev-up.sh` (recomendado, también en Windows con Git Bash) o con los scripts originales:

```bash
./start-microservices.sh            # todos en segundo plano
./start-microservices.sh ms-auth    # uno en primer plano
./stop-microservices.sh             # detener
```

El gateway se inicia con `./gradlew :api-gateway:bootRun` (dev-up.sh ya lo hace).

### 5. Documentación Swagger

| Servicio | Swagger UI |
|---|---|
| ms-auth | http://localhost:8081/swagger-ui.html |
| ms-admin | http://localhost:8082/swagger-ui.html |
| ms-education | http://localhost:8083/swagger-ui.html |

Las rutas de Swagger no requieren token.

Cada petición protegida comprueba la cuenta actual en ms-auth. Configurar `AUTH_SERVICE_URL` en el
gateway, ms-admin y ms-education; dentro de Docker usar el nombre `ms-auth` (el compose ya lo configura).
Si ms-auth no responde, el acceso protegido devuelve 503. Cambiar contraseña, permisos o estado de la
cuenta invalida los tokens existentes; los tokens emitidos antes de esta comprobación requieren nuevo login.
`JWT_EXPIRATION` está en milisegundos: 3600000 corresponde a una hora.

### 6. Verificar que Todo Funciona

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
```

Si todos responden `{"status":"UP"}`, el sistema está funcionando. El health es público.

### Despliegue con Docker

El compose de producción incluye el gateway y conserva los archivos en el volumen `uploaded_files`.
Nginx dirige `api.auth.gemsinnovations.com` al gateway (una sola base para el front:
`https://api.auth.gemsinnovations.com/api/v1`). Los dominios separados anteriores se conservan para
compatibilidad. Adaptar dominios, certificados, `FRONTEND_URL` y CORS al entorno elegido.
Las imágenes compilan únicamente `bootJar`; los tests se ejecutan en el paso de CI. El contexto Docker
excluye `.env`, logs, archivos subidos y salidas de compilación. No se ha realizado ningún despliegue.

## 📋 Tecnologías Utilizadas

- **Spring Boot 3.4.5**, **Spring WebFlux** y **Spring Cloud Gateway**
- **R2DBC** + **PostgreSQL**
- **Redis**: rate limiting
- **JWT** (jjwt, HS512): autenticación; el token lleva `role` e `institutionId`
- **SpringDoc OpenAPI 2.7.0**: Swagger
- **Spring Mail**: correos de recuperación de contraseña (opcional)
- **Gradle** y **Docker Compose**

## 🔧 Desarrollo

### Estructura del Proyecto

```
gems-lms-api/
├── api-gateway/      # Entrada única (8080)
├── ms-auth/          # Autenticación y usuarios
├── ms-admin/         # Instituciones
├── ms-education/     # Contenidos y aprendizaje
├── shared/           # Seguridad y utilidades comunes
├── seed/             # Datos de prueba (cursos)
├── docs/             # Integración con el front
├── docker-compose.yml / docker-compose-local.yml
├── dev-up.sh / seed-dev.sh
└── start-microservices.sh / stop-microservices.sh
```

### Compilar y Ejecutar Tests

```bash
./gradlew build
```

Para ejecutar solo los tests (más rápido: sin reporte ni verificación de cobertura):

```bash
./gradlew test
```

`./gradlew build` exige un mínimo de cobertura de líneas (`coverageMinimum` en `gradle.properties`,
50% por defecto; se puede cambiar con `-PcoverageMinimum=0.7`). Los módulos compilan en paralelo y
Gradle reutiliza resultados en caché, así que una corrida sin cambios tarda unos segundos.

### Paginación

`GET /users/institution/{id}`, `/learning-paths` y `/enrollments/institution/{id}` aceptan `page` y `limit`
(máx. 100) y devuelven el total en el header `X-Total-Count`. Sin `page` responden la lista completa.

### Ver Logs en Tiempo Real

```bash
tail -f logs/ms-auth.log logs/ms-admin.log logs/ms-education.log logs/api-gateway.log
```

## 📝 Notas Importantes

- Cada microservicio tiene su propia base de datos PostgreSQL
- Los archivos subidos quedan en `FILES_DIR` (fuera de git); para S3 u otro almacenamiento basta otra
  implementación de `FileStorage` en ms-education
- El proyecto usa programación reactiva (WebFlux) en todos los microservicios
