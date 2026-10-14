#!/bin/sh
# Deja listo en la VM el subdominio de cada institución listada en ops/institutions.txt: su certificado
# (Let's Encrypt, validación HTTP) y su bloque de nginx. Lo ejecuta el despliegue (ci-cd.yml) en cada push a main;
# también se puede correr a mano:  sh ops/sync-institutions.sh
#
# Pensado para no afectar a los demás sitios de la VM (es un servidor compartido):
#  - Idempotente: si todo está al día no emite, no escribe ni recarga nada.
#  - Solo toca archivos propios: sites-available|enabled/gems-lms-inst-<slug> y gems-lms-instituciones.
#  - Valida con `nginx -t` antes de recargar y, si falla, deshace sus cambios.
#  - Nunca hace fallar el despliegue: ante cualquier problema avisa y sale con 0.
#
# Si existe el certificado comodín (wildcard.<dominio>), un solo bloque atiende a todas las instituciones y no se
# emite ningún certificado por institución (ver docs/deployment.md).
#
# Variables opcionales: CERTBOT_EMAIL (si falta, certbot reutiliza la cuenta ya registrada), INSTITUTION_BASE_DOMAIN
# (por defecto gemsinnovations.com), DEPLOY_ENV (solo actúa en pdn), INSTITUTIONS_FILE.
set -u

here=$(cd "$(dirname "$0")" && pwd)
base=${INSTITUTION_BASE_DOMAIN:-gemsinnovations.com}
list=${INSTITUTIONS_FILE:-$here/institutions.txt}
nginx_dir=${NGINX_DIR:-/etc/nginx}
le_dir=${LE_DIR:-/etc/letsencrypt}
SUDO=${SUDO_CMD-sudo -n}   # SUDO_CMD vacío = sin sudo (pruebas)
reserved=" www lms api app admin auth edu mail static assets cdn login status docs blog soporte gems dev qa pdn prod staging test preview hub "

say() { echo "sync-institutions: $*"; }
warn() { echo "sync-institutions: AVISO: $*" >&2; }

if [ "${DEPLOY_ENV:-pdn}" != pdn ] && [ -z "${FORCE:-}" ]; then
  say "ambiente ${DEPLOY_ENV:-}: solo se aplica en pdn; nada que hacer."
  exit 0
fi
for cmd in certbot nginx; do
  command -v "$cmd" >/dev/null 2>&1 || { warn "no hay $cmd en esta máquina; se omite."; exit 0; }
done
# shellcheck disable=SC2086
$SUDO true 2>/dev/null || { warn "el usuario no tiene sudo sin contraseña; se omite (ver docs/deployment.md)."; exit 0; }
# La configuración efectiva (nginx -T sigue los enlaces de sites-enabled y los include) debe definir el upstream del front.
# shellcheck disable=SC2086
$SUDO nginx -T 2>/dev/null | grep -q "upstream gems_lms_web" ||
  { warn "no encuentro el upstream gems_lms_web en la configuración de nginx (¿está instalado el sitio del LMS?); se omite."; exit 0; }
[ -f "$list" ] || { warn "no existe $list; se omite."; exit 0; }

work=$(mktemp -d) || exit 0
trap 'rm -rf "$work"' EXIT
changed="$work/changed"   # una línea por archivo tocado: nombre
: > "$changed"

# install_conf <nombre> <archivo-renderizado>: instala o actualiza solo si cambió, guardando el original.
install_conf() {
  name=$1; src=$2
  target="$nginx_dir/sites-available/$name"
  link="$nginx_dir/sites-enabled/$name"
  if [ -f "$target" ] && cmp -s "$src" "$target" && [ -L "$link" ]; then
    return 0
  fi
  if [ -f "$target" ]; then cp "$target" "$work/$name.bak"; fi
  # shellcheck disable=SC2086
  $SUDO cp "$src" "$target" && $SUDO ln -sf "$target" "$link" || { warn "no pude escribir $name."; return 1; }
  echo "$name" >> "$changed"
  say "bloque de nginx actualizado: $name"
}

rollback() {
  while read -r name; do
    if [ -f "$work/$name.bak" ]; then
      # shellcheck disable=SC2086
      $SUDO cp "$work/$name.bak" "$nginx_dir/sites-available/$name"
    else
      # shellcheck disable=SC2086
      $SUDO rm -f "$nginx_dir/sites-enabled/$name" "$nginx_dir/sites-available/$name"
    fi
  done < "$changed"
}

# shellcheck disable=SC2086
if $SUDO test -f "$le_dir/live/wildcard.$base/fullchain.pem"; then
  say "certificado comodín presente: un solo bloque atiende a todas las instituciones."
  install_conf gems-lms-instituciones "$here/host-nginx/gems-lms-instituciones.conf" || true
  wildcard=yes
else
  wildcard=no
fi

slugs=$(sed 's/#.*//' "$list" | tr -s ' \t\r' '\n\n\n' | grep -v '^$' || true)
issued=0; skipped=0
for slug in $slugs; do
  if ! printf %s "$slug" | grep -Eq '^[a-z0-9][a-z0-9-]{1,38}[a-z0-9]$'; then
    warn "slug inválido «$slug»: se omite."; skipped=$((skipped + 1)); continue
  fi
  case "$reserved" in *" $slug "*) warn "slug reservado «$slug»: se omite."; skipped=$((skipped + 1)); continue ;; esac
  [ "$wildcard" = yes ] && continue
  domain="$slug.$base"

  # shellcheck disable=SC2086
  if ! $SUDO test -f "$le_dir/live/$domain/fullchain.pem"; then
    say "emitiendo certificado de $domain ..."
    email_arg=""
    [ -n "${CERTBOT_EMAIL:-}" ] && email_arg="-m $CERTBOT_EMAIL"
    # shellcheck disable=SC2086
    if ! $SUDO certbot certonly --nginx -n --agree-tos $email_arg --keep-until-expiring \
         --deploy-hook "systemctl reload nginx" --cert-name "$domain" -d "$domain"; then
      warn "no se pudo emitir el certificado de $domain (¿su DNS apunta a esta VM y el puerto 80 está abierto?); se omite."
      skipped=$((skipped + 1)); continue
    fi
    issued=$((issued + 1))
  fi

  sed "s/__DOMAIN__/$domain/g" "$here/host-nginx/institucion.conf.template" > "$work/gems-lms-inst-$slug"
  install_conf "gems-lms-inst-$slug" "$work/gems-lms-inst-$slug" || skipped=$((skipped + 1))
done

if [ -s "$changed" ]; then
  # shellcheck disable=SC2086
  if $SUDO nginx -t >/dev/null 2>"$work/nginx-test.log"; then
    # shellcheck disable=SC2086
    $SUDO systemctl reload nginx && say "nginx recargado." || warn "no pude recargar nginx."
  else
    warn "nginx -t falló con los cambios; los deshago para no afectar los demás sitios:"
    sed 's/^/    /' "$work/nginx-test.log" >&2
    rollback
    # shellcheck disable=SC2086
    $SUDO nginx -t >/dev/null 2>&1 || warn "nginx -t sigue fallando tras deshacer: el problema ya existía antes de este cambio."
  fi
else
  say "todo al día (certificados emitidos: $issued, omitidos: $skipped)."
fi
exit 0
