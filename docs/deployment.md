# Despliegue seguro

El compose de producción mantiene PostgreSQL y Redis únicamente en la red privada de Docker. Solo
Nginx publica los puertos 80 y 443. Swagger y Prometheus se bloquean en Nginx; Prometheus continúa
disponible para el stack privado de monitoreo.

## Preparar un servidor

1. Copiar `.env.production.example` como `.env` en el directorio de la aplicación.
2. Reemplazar cada `CHANGE_ME`, usar contraseñas diferentes para cada base y generar
   `JWT_SECRET` con al menos 64 bytes aleatorios.
3. Configurar el origen HTTPS real en `CORS_ALLOWED_ORIGINS` y `FRONTEND_URL`.
4. Configurar SMTP y elegir almacenamiento local persistente o S3.
5. Instalar los certificados en las rutas declaradas por `nginx/conf.d/app.conf`.
6. Validar antes de iniciar:

```bash
sh ops/validate-production-env.sh .env
docker compose --env-file .env config --quiet
docker compose --env-file .env up -d
```

La automatización no crea ni reemplaza `.env`: valida el archivo administrado en el servidor antes
de descargar imágenes. Así, una contraseña con caracteres especiales no se interpreta dentro de un
script y una variable ausente detiene el despliegue.

## Comprobaciones posteriores

Todo dominio público de API, incluidos los alias históricos `api.admin` y `api.edu`, debe resolver
al `api-gateway`. Nginx no publica los microservicios directamente: así se conservan el rate limit,
la validación de sesión y la auditoría para todas las solicitudes.
El compose espera que bases, Redis, microservicios y gateway estén saludables antes de habilitar la
siguiente capa; un arranque lento no expone Nginx contra servicios que todavía no están listos.
Al actualizar o detener contenedores, Spring dispone de hasta 20 segundos para terminar solicitudes
en curso antes de que Docker complete el apagado.

- `https://<api>/actuator/health` responde `UP`.
- `/swagger-ui`, `/v3/api-docs` y `/actuator/prometheus` responden 404 desde Internet.
- Los puertos 5432–5434 y 6379 no están publicados por Docker.
- Los encabezados incluyen HSTS, `nosniff`, protección contra marcos y política de referencia.
- Una recuperación de contraseña llega al proveedor SMTP y el enlace usa el dominio HTTPS del front.
- Un archivo subido continúa disponible después de recrear los contenedores.

Respaldos, restauración y métricas están documentados en `docs/backups.md` y `docs/monitoring.md`.

## Despliegue automático en la VM de Oracle (GitHub Actions)

La VM de Oracle (ARM Ampere) es **compartida** con otros servicios. Por eso el borde es el **nginx del sistema** y no el
contenedor `nginx` del compose (queda en el perfil `edge`, para una VM dedicada).

```
Internet → nginx del sistema (TLS, ops/host-nginx/gems-lms.conf)
             ├─ lms.gemsinnovations.com        → 127.0.0.1:18081 → web (front estático)
             └─ api.{auth,admin,edu}.gems…     → 127.0.0.1:18080 → api-gateway → microservicios
```

Los repos publican imágenes `linux/arm64` en `ghcr.io/gems-innovations` (compiladas en runners ARM
de GitHub, con `GITHUB_TOKEN`: no hace falta Docker Hub). La VM necesita `docker login ghcr.io` si los paquetes son privados.

| Repo | Workflow | Qué hace |
|---|---|---|
| gems-lms-api | `.github/workflows/ci-cd.yml` | Tests, imágenes de `ms-auth`, `ms-admin`, `ms-education`, `api-gateway`; copia el compose a la VM y hace `up -d` |
| gems-lms-web | `.github/workflows/deploy.yml` | Tests, imagen `gems-web` (build `production,static` + nginx) y `up -d web` |

Ramas: `develop` → `dev`, `qa` → `qa`, `main` → `pdn` (etiqueta `<entorno>-latest`).

### Configuración en GitHub (en ambos repos, Environment `pdn`)

Secrets: `VM_HOST` (IP pública), `VM_USER`, `VM_SSH_KEY` (llave privada sin passphrase,
idealmente una llave solo para despliegue agregada a `~/.ssh/authorized_keys` de la VM).

### Preparar la VM (una sola vez)

1. Apuntar en el DNS `lms`, `api.auth`, `api.admin` y `api.edu` a la IP pública (registros A).
2. Copiar el repo de ops a `~/app` y crear `~/app/.env` desde `.env.production.example`
   (el workflow nunca lo crea ni lo toca).
3. Ejecutar `sh ~/app/ops/setup-vm.sh <correo>`: emite los certificados con el certbot del sistema
   (renovación por `certbot.timer`), instala el sitio y valida con `nginx -t` antes de recargar.

Primer despliegue: publica primero el front (la imagen `gems-web` debe existir) y luego el API.

### Datos iniciales

El primer super admin se crea solo al arrancar `ms-auth` con `BOOTSTRAP_SUPERADMIN_EMAIL` /
`BOOTSTRAP_SUPERADMIN_PASSWORD`. Los cursos gratuitos se cargan con los scripts de `seed/` contra la
URL pública del API.
