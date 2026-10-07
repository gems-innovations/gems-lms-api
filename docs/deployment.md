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

Todo corre en una sola VM con Docker Compose: Nginx (TLS) → `web` (front estático) y → `api-gateway`
→ microservicios. Los repos publican imágenes en Docker Hub y la VM las descarga por SSH.

| Repo | Workflow | Qué hace |
|---|---|---|
| gems-lms-api | `.github/workflows/ci-cd.yml` | Tests, imágenes de `ms-auth`, `ms-admin`, `ms-education`, `api-gateway`; copia compose y nginx a la VM y hace `up -d` |
| gems-lms-web | `.github/workflows/deploy.yml` | Tests, imagen `gems-web` (build `production,static` + nginx) y `up -d web` |

Ramas: `develop` → `dev`, `qa` → `qa`, `main` → `pdn` (etiqueta `<entorno>-latest`). Cada rama usa su
GitHub Environment, así que cada uno puede tener su propia VM.

### Configuración en GitHub (en ambos repos, por Environment)

- Secrets: `DOCKER_USERNAME`, `DOCKER_PASSWORD` (token de Docker Hub), `VM_HOST` (IP pública),
  `VM_USER` (`ubuntu` u `opc`), `VM_SSH_KEY` (llave privada, sin passphrase).
- Variable opcional `DOCKER_PLATFORMS`: `linux/amd64` (defecto) o `linux/amd64,linux/arm64` si la VM es
  ARM (Ampere). El build ARM bajo emulación es lento.
- Las imágenes deben ser públicas o la VM debe haber hecho `docker login`.

### Preparar la VM (una sola vez)

1. En Oracle Cloud abrir TCP 80 y 443 en la Security List / NSG de la subred.
2. Apuntar los DNS del front y del API a la IP pública.
3. Ejecutar `ops/setup-vm.sh <dominio-front> <dominio-api> <correo>`: instala Docker, abre el
   firewall local, emite los certificados y programa la renovación.
4. Crear `~/app/.env` desde `.env.production.example`. El workflow nunca lo crea ni lo toca.
5. Si el dominio del front no es `lms.gemsinnovations.com`, editar `nginx/conf.d/web.conf`.

El orden del primer despliegue es: push del API (levanta bases y servicios; Nginx espera al `web`),
y luego push del front. Si el front aún no se publicó, `docker compose up` fallará al no encontrar
`gems-web`: publica primero el front o ejecuta ambos workflows.

### Datos iniciales

Los cursos gratuitos se cargan manualmente con los scripts de `seed/` contra la URL pública del API.
