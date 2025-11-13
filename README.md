# GEMS LMS API

Sistema de gestión de aprendizaje (LMS) basado en arquitectura de microservicios con Spring Boot, WebFlux y PostgreSQL.

## 🏗️ Arquitectura

### Componentes Principales

El proyecto sigue una **arquitectura de microservicios** basada en **Clean Architecture + DDD (Domain-Driven Design)**:

- **Microservicios**: 
  - `ms-auth`: Gestión de autenticación y usuarios
  - `ms-admin`: Gestión administrativa
  - `ms-education`: Gestión educativa
- **Bases de Datos**: Una base de datos PostgreSQL independiente por microservicio
- **Redis**: Servicio de caché y rate limiting
- **Shared Module**: Componentes compartidos entre microservicios

### Estructura de Microservicios

Cada microservicio sigue la estructura de Clean Architecture:

```
ms-[nombre]/
├── domain/              # Capa de dominio (entidades, value objects, excepciones)
├── application/         # Capa de aplicación (use cases, gateways, commands/queries)
└── infrastructure/      # Capa de infraestructura (repositorios, controladores, config)
```

## 📊 Flujo de Información

### 1. Entrada de Petición

Cuando una petición HTTP llega a un microservicio, el flujo es el siguiente:

```
Cliente → Microservicio (Puerto específico)
```

### 2. Procesamiento en el Microservicio

Cada microservicio procesa las peticiones a través de una cadena de filtros:

#### Filtros Aplicados (en orden):

1. **SecurityHeadersFilter**: Agrega headers de seguridad HTTP
2. **RateLimitFilter**: Controla el límite de peticiones por cliente usando Redis
3. **JwtAuthenticationFilter**: Valida el token JWT
   - Si la ruta es de login o Swagger, permite el acceso sin token
   - Para otras rutas, extrae y valida el token JWT del header `Authorization`
   - Si es válido, agrega headers internos: `X-User-Id` y `X-User-Role`
   - Si no es válido, retorna `401 Unauthorized`

### 3. Procesamiento de la Petición

1. **Controlador REST**: Recibe la petición y utiliza los **Use Cases** para procesar la lógica de negocio
2. **Use Case**: Ejecuta la lógica de negocio utilizando los **Gateways** (interfaces)
3. **Repositorio Adapter**: Implementa los gateways y accede a la base de datos usando R2DBC (programación reactiva)
4. **Respuesta**: Retorna la respuesta al cliente

## 🚀 Guía de Ejecución

### Prerrequisitos

- Java 24
- Docker y Docker Compose
- Bash (para ejecutar los scripts en Linux/Mac) o Git Bash/WSL (para Windows)

### 1. Clonar el Repositorio

```bash
git clone <repository-url>
cd gems-lms-api
```

### 2. Subir Docker Compose

El archivo `docker-compose.yml` contiene las configuraciones de las bases de datos y Redis. **NO modificar este archivo**.

Ejecutar:

```bash
docker-compose up -d
```

Esto iniciará:
- `postgres-auth` en el puerto **5432**
- `postgres-admin` en el puerto **5433**
- `postgres-education` en el puerto **5434**
- `redis` en el puerto **6379**

### 3. Crear las Tablas en PostgreSQL

Conectarse a cada instancia de PostgreSQL y ejecutar los scripts `schema.sql` correspondientes.

#### Base de Datos Auth (Puerto 5432)

```bash
docker exec -i gems-postgres-auth psql -U auth_user -d auth_db < ms-auth/src/main/resources/schema.sql
```

#### Base de Datos Admin (Puerto 5433)

```bash
docker exec -i gems-postgres-admin psql -U admin_user -d admin_db < ms-admin/src/main/resources/schema.sql
```

#### Base de Datos Education (Puerto 5434)

```bash
docker exec -i gems-postgres-education psql -U education_user -d education_db < ms-education/src/main/resources/schema.sql
```

### 4. Crear el Archivo .env

Crear un archivo `.env` en la raíz del proyecto con las siguientes variables de entorno:

```env
AUTH_PORT=8081
ADMIN_PORT=8082
EDUCATION_PORT=8083

JWT_SECRET=your-jwt-secret-key-here-change-in-production
JWT_EXPIRATION=3600000

AUTH_LOGIN_PATH=/api/v1/users/login

REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=your-redis-password

RATE_LIMIT_REQUESTS=100
RATE_LIMIT_WINDOW=60
RATE_LIMIT_KEY_PREFIX=rate_limit:

AUTH_DB_NAME=auth_db
AUTH_DB_USER=auth_user
AUTH_DB_PASSWORD=auth_password
AUTH_R2DBC_URL=r2dbc:postgresql://localhost:5432/auth_db
AUTH_R2DBC_USERNAME=auth_user
AUTH_R2DBC_PASSWORD=auth_password

ADMIN_DB_NAME=admin_db
ADMIN_DB_USER=admin_user
ADMIN_DB_PASSWORD=admin_password
ADMIN_R2DBC_URL=r2dbc:postgresql://localhost:5433/admin_db
ADMIN_R2DBC_USERNAME=admin_user
ADMIN_R2DBC_PASSWORD=admin_password

EDUCATION_DB_NAME=education_db
EDUCATION_DB_USER=education_user
EDUCATION_DB_PASSWORD=education_password
EDUCATION_R2DBC_URL=r2dbc:postgresql://localhost:5434/education_db
EDUCATION_R2DBC_USERNAME=education_user
EDUCATION_R2DBC_PASSWORD=education_password

CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:4200
CORS_ALLOWED_METHODS=GET,POST,PUT,DELETE,OPTIONS
CORS_ALLOWED_HEADERS=*
CORS_ALLOW_CREDENTIALS=true
CORS_MAX_AGE=3600
```

**Importante**: Cambiar los valores de ejemplo por valores seguros en producción.

### 5. Compilar el Proyecto

Antes de ejecutar los microservicios, compilar el proyecto:

```bash
./gradlew build -x jacocoTestCoverageVerification
```

### 6. Ejecutar los Microservicios

#### Iniciar Todos los Microservicios

Ejecutar el script `start-microservices.sh`:

```bash
chmod +x start-microservices.sh
./start-microservices.sh
```

O especificar que se inicien todos:

```bash
./start-microservices.sh all
```

Esto iniciará todos los microservicios en segundo plano:
- ms-auth (puerto 8081)
- ms-admin (puerto 8082)
- ms-education (puerto 8083)

Los logs se guardarán en el directorio `logs/`:
- `logs/auth.log`
- `logs/admin.log`
- `logs/education.log`

#### Iniciar un Microservicio Específico

Para iniciar solo un microservicio en primer plano (ver logs en consola):

```bash
./start-microservices.sh ms-auth
./start-microservices.sh ms-admin
./start-microservices.sh ms-education
```

**Nota**: Cuando se ejecuta un microservicio individual, los logs se muestran en la consola y se puede detener con `Ctrl+C`.

#### Detener los Microservicios

Para detener todos los microservicios:

```bash
chmod +x stop-microservices.sh
./stop-microservices.sh
```

O para detener uno específico:

```bash
./stop-microservices.sh ms-auth
./stop-microservices.sh ms-admin
./stop-microservices.sh ms-education
```

**Nota**: Los scripts **NO deben modificarse**.

### 7. Acceder a la Documentación Swagger

Cada microservicio tiene documentación Swagger disponible. Para acceder:

#### ms-auth (Puerto 8081)

- **Swagger UI**: `http://localhost:8081/swagger-ui.html`
- **API Docs JSON**: `http://localhost:8081/v3/api-docs`

#### ms-admin (Puerto 8082)

- **Swagger UI**: `http://localhost:8082/swagger-ui.html`
- **API Docs JSON**: `http://localhost:8082/v3/api-docs`

#### ms-education (Puerto 8083)

- **Swagger UI**: `http://localhost:8083/swagger-ui.html`
- **API Docs JSON**: `http://localhost:8083/v3/api-docs`

**Nota**: Las rutas de Swagger están excluidas de la autenticación JWT, por lo que puedes acceder sin token.

### 8. Verificar que Todo Funciona

Probar los endpoints de health de cada microservicio:

#### ms-auth
```bash
curl http://localhost:8081/actuator/health
```

#### ms-admin
```bash
curl http://localhost:8082/actuator/health
```

#### ms-education
```bash
curl http://localhost:8083/actuator/health
```

Si todos responden con estado `UP`, el sistema está funcionando correctamente.

## 📋 Tecnologías Utilizadas

- **Spring Boot 3.4.5**: Framework principal
- **Spring WebFlux**: Programación reactiva
- **R2DBC**: Acceso reactivo a base de datos
- **PostgreSQL**: Base de datos relacional
- **Redis**: Caché y rate limiting
- **JWT**: Autenticación y autorización
- **SpringDoc OpenAPI 2.7.0**: Documentación de API (Swagger)
- **Gradle**: Gestión de dependencias y construcción
- **Docker & Docker Compose**: Contenedores y orquestación

## 🔧 Desarrollo

### Estructura del Proyecto

```
gems-lms-api/
├── ms-auth/          # Microservicio de autenticación
├── ms-admin/         # Microservicio administrativo
├── ms-education/     # Microservicio educativo
├── shared/           # Módulo compartido (filtros, configuraciones)
├── docker-compose.yml
├── build.gradle
├── settings.gradle
├── start-microservices.sh
└── stop-microservices.sh
```

### Compilar y Ejecutar Tests

```bash
./gradlew build
```

Para ejecutar solo los tests sin verificar cobertura:

```bash
./gradlew test
```

### Ver Logs en Tiempo Real

Para ver los logs de un microservicio en tiempo real:

```bash
tail -f logs/auth.log
tail -f logs/admin.log
tail -f logs/education.log
```

## 📝 Notas Importantes

- **NO modificar** el archivo `docker-compose.yml`
- **NO modificar** los scripts `start-microservices.sh` y `stop-microservices.sh`
- Las rutas de Swagger (`/swagger-ui/**`, `/api-docs/**`, `/webjars/**`) están excluidas de la autenticación JWT
- Cada microservicio tiene su propia base de datos PostgreSQL
- Redis se usa para rate limiting y caché
- El proyecto usa programación reactiva (WebFlux) en todos los microservicios
