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

- `https://<api>/actuator/health` responde `UP`.
- `/swagger-ui`, `/v3/api-docs` y `/actuator/prometheus` responden 404 desde Internet.
- Los puertos 5432–5434 y 6379 no están publicados por Docker.
- Los encabezados incluyen HSTS, `nosniff`, protección contra marcos y política de referencia.
- Una recuperación de contraseña llega al proveedor SMTP y el enlace usa el dominio HTTPS del front.
- Un archivo subido continúa disponible después de recrear los contenedores.

Respaldos, restauración y métricas están documentados en `docs/backups.md` y `docs/monitoring.md`.
