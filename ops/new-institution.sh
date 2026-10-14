#!/bin/sh
# Da de alta una institución con su subdominio: la crea con su marca, le asigna el slug
# (<slug>.gemsinnovations.com) y, si quieres, los cursos públicos que muestra su landing.
# Pensado para producción, pero sirve igual contra el API local. Necesita el super admin.
#
#   API=https://api.auth.gemsinnovations.com/api/v1 SUPER_EMAIL=super@gems.lms \
#     sh ops/new-institution.sh <id> "<Nombre>" <slug> <#primario> <#secundario> <oscuro|claro> [<idCurso>...]
#
#   sh ops/new-institution.sh knowledge-craft "Knowledge Craft" knowledgecraft '#3FD0D4' '#5DBB47' oscuro 19
#
# La contraseña se lee de SUPER_PASSWORD o se pide sin mostrarla. Los cursos deben ser del catálogo abierto
# (los de «cursos gratis»); un curso puede mostrarse en la landing de una sola institución.
# Es seguro repetirlo: si la institución ya existe, solo se actualizan el slug y los cursos.
set -eu

[ $# -ge 6 ] || { sed -n '2,13p' "$0" >&2; exit 1; }
id=$1; name=$2; slug=$3; primary=$4; secondary=$5; theme=$6
shift 6

: "${API:?Define API (p. ej. https://api.auth.gemsinnovations.com/api/v1)}"
: "${SUPER_EMAIL:?Define SUPER_EMAIL}"
API=${API%/}

case "$name$id$slug" in *\"* | *\\*) echo "El nombre, el id y el slug no pueden llevar comillas ni barras." >&2; exit 1 ;; esac
printf %s "$slug" | grep -Eq '^[a-z0-9][a-z0-9-]{1,38}[a-z0-9]$' ||
  { echo "Slug inválido: 3 a 40 caracteres, minúsculas, números y guiones (sin empezar ni terminar con guion)." >&2; exit 1; }
for c in "$primary" "$secondary"; do
  printf %s "$c" | grep -Eq '^#[0-9A-Fa-f]{6}$' || { echo "Color inválido: $c (usa #RRGGBB)." >&2; exit 1; }
done
case "$theme" in oscuro) dark=true ;; claro) dark=false ;; *) echo "El tema es oscuro o claro." >&2; exit 1 ;; esac

if [ -z "${SUPER_PASSWORD:-}" ]; then
  printf 'Contraseña de %s: ' "$SUPER_EMAIL" >&2
  stty -echo 2>/dev/null || true
  read -r SUPER_PASSWORD
  stty echo 2>/dev/null || true
  echo >&2
fi

# Los cuerpos van por stdin: así la contraseña no queda en la lista de procesos.
token=$(printf '{"email":"%s","password":"%s"}' "$SUPER_EMAIL" "$SUPER_PASSWORD" |
  curl -fsS -X POST "$API/auth/login" -H 'Content-Type: application/json' --data-binary @- |
  sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
[ -n "$token" ] || { echo "No se pudo iniciar sesión como $SUPER_EMAIL." >&2; exit 1; }

call() { # método ruta [cuerpo por stdin]; imprime el código HTTP
  curl -s -o /dev/null -w '%{http_code}' -X "$1" "$API/$2" -H "Authorization: Bearer $token" \
    -H 'Content-Type: application/json; charset=utf-8' --data-binary @-
}

code=$(cat <<JSON | call POST institutions
{"id":"$id","name":"$name","type":"academy","status":"active",
 "metadata":{"description":"$name","maxUsers":5000,"subscriptionType":"premium"},
 "branding":{"type":"logo-text","colorPrimary":"$primary","colorSecondary":"$secondary","darkMode":$dark}}
JSON
)
created=no
case "$code" in
  201) created=yes; echo "Institución $id creada." ;;
  400|409) echo "La institución $id ya existe: se conserva como está." ;;
  *) echo "No se pudo crear la institución (HTTP $code)." >&2; exit 1 ;;
esac

code=$(printf '{"slug":"%s"}' "$slug" | call PUT "home/$id/slug")
case "$code" in
  200) echo "Subdominio asignado: https://$slug.gemsinnovations.com" ;;
  *)
    [ "$code" = 409 ] && echo "El slug $slug ya lo usa otra institución." >&2 || echo "No se pudo asignar el slug (HTTP $code)." >&2
    # No dejar una institución a medias: se deshace solo lo que este comando acaba de crear.
    if [ "$created" = yes ]; then
      printf '' | call DELETE "institutions/$id" >/dev/null && echo "Se deshizo la creación de $id." >&2
    fi
    exit 1 ;;
esac

for course in "$@"; do
  code=$(printf '{"institutionId":"%s"}' "$id" | call PUT "courses/$course/showcase")
  [ "$code" = 204 ] && echo "Curso $course asignado a la landing." ||
    echo "No se pudo asignar el curso $course (HTTP $code): debe existir y ser del catálogo abierto." >&2
done

echo "Listo. Falta, si no lo has hecho, el DNS y el certificado comodín (docs/deployment.md)."
