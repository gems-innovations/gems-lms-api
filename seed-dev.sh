#!/bin/bash
# Seed de datos para desarrollo local (vía api-gateway en :8080).
# Crea los mismos usuarios/instituciones que el front usaba como mocks, para que
# el login y las pantallas funcionen contra el back real.
# Es idempotente: si un registro ya existe, el back responde 409 (400 para
# instituciones, código INSTITUTION_ALREADY_EXISTS) y se continúa.
# Requiere Node.js para el catálogo de cursos (paso 4).
#
# Contraseña de todos los usuarios de desarrollo: la de DEV_PASSWORD.
#
# Los cuerpos JSON se envían por stdin (--data-binary @-): en Git Bash para Windows
# los argumentos de línea de comandos se recodifican y rompen las tildes.

BASE="${BASE:-http://localhost:8080/api/v1}"
DEV_PASSWORD="${DEV_PASSWORD:-GemsDev2026!}"

json_field() { sed -n "s/.*\"$1\":\"\{0,1\}\([^\",}]*\)\"\{0,1\}.*/\1/p"; }

# post <ruta> [token]  — cuerpo por stdin, imprime el código HTTP
post() {
  local auth=()
  [ -n "$2" ] && auth=(-H "Authorization: Bearer $2")
  curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/$1" "${auth[@]}" \
    -H 'Content-Type: application/json; charset=utf-8' --data-binary @-
}

register() { # firstName lastName username email role institutionId
  local inst_json="null"; [ -n "$6" ] && inst_json="\"$6\""
  echo "  usuario $4 ($5) -> $(post auth/register <<JSON
{"firstName":"$1","lastName":"$2","username":"$3","email":"$4","password":"$DEV_PASSWORD","role":"$5","institutionId":$inst_json}
JSON
)"
}

echo "1) Usuarios"
register Super   Admin   superadmin     super@gems.lms          SUPER_ADMIN ""
register Carlos  Ramírez carlos.ramirez admin@unal.edu.co       ADMIN       inst-1
register Lucía   Gómez   lucia.gomez    admin@pragma.co         ADMIN       inst-2
register Andrés  Torres  andres.torres  instructor@unal.edu.co  INSTRUCTOR  inst-1
register María   López   maria.lopez    estudiante@unal.edu.co  STUDENT     inst-1

TOKEN=$(curl -s -X POST "$BASE/auth/login" -H 'Content-Type: application/json' --data-binary @- <<JSON | json_field token
{"email":"super@gems.lms","password":"$DEV_PASSWORD"}
JSON
)
if [ -z "$TOKEN" ]; then echo "No se pudo iniciar sesión como super@gems.lms"; exit 1; fi

echo "2) Instituciones"
echo "  inst-1 -> $(post institutions "$TOKEN" <<'JSON'
{"id":"inst-1","name":"Universidad Nacional de Colombia","type":"university","status":"active",
 "metadata":{"description":"Principal universidad pública de Colombia, referente en investigación y educación superior.","website":"https://unal.edu.co","contactEmail":"info@unal.edu.co","phoneNumber":"+57 1 316 5000","address":"Carrera 45 #26-85, Bogotá, Colombia","maxUsers":50000,"subscriptionType":"enterprise"},
 "branding":{"type":"color-badge","colorPrimary":"#6C63FF","colorSecondary":"#1E1B4B","darkMode":true}}
JSON
)"
echo "  inst-2 -> $(post institutions "$TOKEN" <<'JSON'
{"id":"inst-2","name":"Pragma","type":"academy","status":"active",
 "metadata":{"description":"Empresa de tecnología e innovación digital con foco en formación técnica especializada.","website":"https://pragma.com.co","contactEmail":"academy@pragma.com.co","phoneNumber":"+57 4 6049090","address":"Medellín, Colombia","maxUsers":2000,"subscriptionType":"premium"},
 "branding":{"type":"logo-text","colorPrimary":"#FF6B35","colorSecondary":"#1a1a2e","darkMode":false}}
JSON
)"

echo "3) Perfil de estudiante en ms-education (las inscripciones usan este id)"
echo "  estudiante@unal.edu.co -> $(post students/register "$TOKEN" <<'JSON'
{"name":"María López","email":"estudiante@unal.edu.co","birthDate":"2000-05-10","country":"Colombia","city":"Bogotá","documentType":"CC","documentNumber":"1000000001"}
JSON
)"

echo "4) Catálogo de cursos de demostración (seed/demo-courses.json)"
BASE="$BASE" DEV_PASSWORD="$DEV_PASSWORD" node "$(dirname "$0")/seed/seed-courses.mjs"

echo "Listo. Usuarios de desarrollo con contraseña DEV_PASSWORD (ver este script)."
