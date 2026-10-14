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

## Instituciones con su subdominio (`<slug>.gemsinnovations.com`)

`lms.gemsinnovations.com` es la portada de GEMS; cada institución (knowledgecraft, unal, pragma, ...) vive en su propio
subdominio, con su marca y, si tiene, su landing. Un solo bloque de nginx atiende a todas: agregar una institución nueva
**no requiere tocar nginx ni certificados**, solo darla de alta.

Una sola vez:

1. **DNS**: registro `*.gemsinnovations.com` (A) hacia la IP de la VM. Los nombres exactos (`lms`, `api.*`) siguen mandando.
2. **Certificado comodín** (Let's Encrypt solo lo emite por desafío DNS): instala el plugin de certbot de tu proveedor de DNS y corre
   `WILDCARD_CERTBOT_ARGS="--dns-<proveedor> --dns-<proveedor>-credentials <archivo>" sh ops/setup-vm.sh tu@correo.com`.
   Emite `wildcard.gemsinnovations.com` y activa `ops/host-nginx/gems-lms-instituciones.conf`; se renueva con `certbot.timer`.
   Sin la variable, el script omite el comodín y los subdominios quedan inactivos (el resto del sitio no cambia).
3. **CORS**: en `~/app/.env`, `CORS_ALLOWED_ORIGINS=https://lms.gemsinnovations.com,https://*.gemsinnovations.com`
   (acepta patrones). Reinicia gateway, ms-auth, ms-admin y ms-education.
4. Desplegar front y back con los cambios de la home por institución (el esquema se actualiza al arrancar).

Por cada institución nueva:

```
API=https://api.auth.gemsinnovations.com/api/v1 SUPER_EMAIL=<super admin> \
  sh ops/new-institution.sh knowledge-craft "Knowledge Craft" knowledgecraft '#3FD0D4' '#5DBB47' oscuro <idCurso>...
```

Crea la institución con su marca, le asigna el slug y los cursos públicos que mostrará su landing. Después crea su
administrador desde el panel. El slug solo lo asigna el super admin. Más detalles en `docs/home-personalizada.md`.

### Sin certificado comodín: un certificado por institución

Mientras no haya comodín (p. ej. el DNS está en GoDaddy), cada institución puede tener su propio certificado por validación
HTTP: solo necesita su registro A hacia la VM (o el comodín `*` A del DNS) y se renueva sola con `certbot.timer`.

```
INSTITUTION_DOMAINS="knowledgecraft.gemsinnovations.com" sh ops/setup-vm.sh tu@correo.com
```

Emite el certificado de ese nombre e instala su bloque de nginx (`ops/host-nginx/institucion.conf.template`). Se puede
repetir con más dominios (separados por espacio). Si después se pasa al certificado comodín, los bloques por nombre siguen
sirviendo: nginx da prioridad al nombre exacto. Esto no reemplaza el alta de la institución (`ops/new-institution.sh`) ni
el CORS.

### Automático en cada despliegue (`ops/institutions.txt`)

Para no entrar a la VM por cada institución, el despliegue a `main` ejecuta `ops/sync-institutions.sh` al final: por cada
slug de `ops/institutions.txt` emite el certificado (si falta) e instala su bloque de nginx. Para sumar una institución:

1. `sh ops/new-institution.sh ...` (alta en el API; se corre desde cualquier computador).
2. Agrega su slug a `ops/institutions.txt` y haz push a `main`.

Garantías para el resto de sitios de la VM (es compartida): es idempotente (si nada cambió no emite, no escribe ni recarga),
solo toca `gems-lms-inst-<slug>` y `gems-lms-instituciones`, valida con `nginx -t` antes de recargar y **deshace sus cambios si
falla**, y nunca hace fallar el despliegue: ante cualquier problema avisa en el log del job. Solo actúa en `pdn`.

Requisitos: el usuario de despliegue con `sudo` sin contraseña (`sudo -n true`; si no lo tiene, el script avisa y se omite);
el DNS de la institución apuntando a la VM y el puerto 80 abierto; opcionalmente la variable de GitHub `CERTBOT_EMAIL` en el
Environment `pdn` (si falta, certbot reutiliza la cuenta ya registrada en la VM). Con certificado comodín, el mismo script instala
el bloque comodín y deja de emitir certificados por institución.
