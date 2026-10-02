#!/bin/bash
# Levanta todo el back en local: PostgreSQL + Redis (docker-compose-local.yml),
# ms-auth, ms-admin, ms-education y api-gateway. Logs en logs/<servicio>.log.
#
#   ./dev-up.sh          # arranca todo
#   ./dev-up.sh --seed   # arranca todo y carga los datos de desarrollo (seed-dev.sh)
#
# Requisitos: Docker, Java 24 (JAVA_HOME) y el archivo .env en esta carpeta.
#
# En Git Bash para Windows, MSYS convierte las variables que parecen rutas POSIX
# (p. ej. AUTH_LOGIN_PATH=/api/v1/auth/login) en rutas de Windows al lanzar java,
# y el filtro JWT deja de reconocer la ruta de login. Por eso se desactiva aquí.
export MSYS_NO_PATHCONV=1
export MSYS2_ENV_CONV_EXCL='*'

set -e
cd "$(dirname "$0")"

if [ ! -f .env ]; then
  echo "Falta el archivo .env (ver README)."; exit 1
fi

while IFS= read -r line || [ -n "$line" ]; do
  line=${line%$'\r'}
  [[ $line =~ ^([A-Za-z_][A-Za-z0-9_]*)=(.*)$ ]] && export "${BASH_REMATCH[1]}=${BASH_REMATCH[2]}"
done < .env

echo "1) Bases de datos y Redis"
docker compose -f docker-compose-local.yml --env-file .env up -d

echo "2) Esperando a que los contenedores estén sanos..."
for _ in $(seq 1 60); do
  healthy=$(docker ps --filter name=gems- --filter health=healthy -q | wc -l)
  [ "$healthy" -ge 4 ] && break
  sleep 2
done

mkdir -p logs
start() { # nombre tarea-gradle puerto
  if curl -s -o /dev/null -m 2 "http://localhost:$3"; then
    echo "   $1 ya está corriendo en :$3"
    return
  fi
  nohup ./gradlew "$2" > "logs/$1.log" 2>&1 &
  echo "   $1 -> logs/$1.log"
}

echo "3) Microservicios"
start ms-auth      :ms-auth:bootRun      "${AUTH_PORT:-8081}"
start ms-admin     :ms-admin:bootRun     "${ADMIN_PORT:-8082}"
start ms-education :ms-education:bootRun "${EDUCATION_PORT:-8083}"
start api-gateway  :api-gateway:bootRun  8080

echo "4) Esperando a que respondan (puede tardar 1-2 min la primera vez)..."
for port in "${AUTH_PORT:-8081}" "${ADMIN_PORT:-8082}" "${EDUCATION_PORT:-8083}" 8080; do
  for _ in $(seq 1 90); do
    curl -s -o /dev/null -m 2 "http://localhost:$port/v3/api-docs" && break
    sleep 2
  done
  code=$(curl -s -o /dev/null -w '%{http_code}' -m 2 "http://localhost:$port/v3/api-docs" || true)
  [ "$code" != "000" ] && echo "   :$port arriba" || echo "   :$port sin respuesta (revisa logs/)"
done

if [ "$1" = "--seed" ]; then
  echo "5) Datos de desarrollo"
  bash ./seed-dev.sh
fi

echo "Listo. Gateway en http://localhost:8080/api/v1 — para detener: ./stop-microservices.sh y docker compose -f docker-compose-local.yml stop"
