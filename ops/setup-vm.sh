#!/bin/sh
# Preparación única de la VM de Oracle (Ubuntu, compartida con otros proyectos).
# El borde es el nginx del sistema: este script emite los certificados con el certbot del sistema
# (que ya se renueva solo con certbot.timer) e instala el sitio ops/host-nginx/gems-lms.conf.
#   sh ops/setup-vm.sh tu@correo.com
# Requisitos previos: los dominios del front y del API apuntan a la IP pública de la VM y la
# Security List / NSG de Oracle permite entrada TCP 22, 80 y 443.
#
# Instituciones (<slug>.gemsinnovations.com). Hay dos formas, combinables:
#
#   a) Un certificado por institución (validación HTTP; solo requiere un registro A hacia la VM y se renueva solo):
#        INSTITUTION_DOMAINS="knowledgecraft.gemsinnovations.com" sh ops/setup-vm.sh tu@correo.com
#      Varios dominios, separados por espacio. Se puede repetir el comando al sumar una institución.
#
#   b) Certificado comodín *.gemsinnovations.com: un solo bloque de nginx atiende a todas las instituciones, sin
#      repetir nada por cada una. Let's Encrypt solo lo emite con el desafío DNS: pasa los argumentos de tu
#      proveedor de DNS (e instala antes su plugin de certbot, p. ej. python3-certbot-dns-cloudflare):
#        WILDCARD_CERTBOT_ARGS="--dns-cloudflare --dns-cloudflare-credentials /etc/letsencrypt/cloudflare.ini" \
#          sh ops/setup-vm.sh tu@correo.com
#
# Sin ninguna de las dos variables el sitio queda como antes (solo lms y el API).
set -eu

[ $# -ge 1 ] || { echo "Uso: $0 <correo-certbot>" >&2; exit 1; }
email=$1
here=$(cd "$(dirname "$0")" && pwd)
web_domain=lms.gemsinnovations.com
api_domains="api.auth.gemsinnovations.com api.admin.gemsinnovations.com api.edu.gemsinnovations.com"

# 1) Docker
if ! command -v docker >/dev/null 2>&1; then
  curl -fsSL https://get.docker.com | sudo sh
  sudo usermod -aG docker "$USER"
  echo "Docker instalado. Cierra la sesión SSH y vuelve a entrar para usarlo sin sudo."
fi

# 2) Firewall local: las imágenes de Oracle traen iptables cerrado salvo el 22
for port in 80 443; do
  sudo iptables -C INPUT -p tcp --dport "$port" -j ACCEPT 2>/dev/null ||
    sudo iptables -I INPUT 5 -p tcp --dport "$port" -j ACCEPT
done

# 3) Carpetas
mkdir -p "$HOME/app"
sudo mkdir -p /var/www/certbot

# 4) Certificados con el plugin nginx del certbot del sistema (no detiene nginx)
dpkg -s python3-certbot-nginx >/dev/null 2>&1 || sudo apt-get install -y python3-certbot-nginx
sudo certbot certonly --nginx -n --agree-tos -m "$email" --keep-until-expiring \
  --cert-name "$web_domain" -d "$web_domain"
api_args=""
for d in $api_domains; do api_args="$api_args -d $d"; done
# shellcheck disable=SC2086
sudo certbot certonly --nginx -n --agree-tos -m "$email" --keep-until-expiring \
  --cert-name api.auth.gemsinnovations.com $api_args

# 4b) Certificado comodín para todas las instituciones (opcional; ver arriba)
if [ -n "${WILDCARD_CERTBOT_ARGS:-}" ]; then
  # shellcheck disable=SC2086
  sudo certbot certonly -n --agree-tos -m "$email" --keep-until-expiring \
    --cert-name wildcard.gemsinnovations.com -d '*.gemsinnovations.com' $WILDCARD_CERTBOT_ARGS
fi

# 4c) Instituciones con certificado propio (opcional; ver arriba): su certificado y su bloque de nginx.
#     Un nombre exacto tiene prioridad sobre el bloque comodín, así que luego se puede pasar al comodín sin cambios.
for d in ${INSTITUTION_DOMAINS:-}; do
  printf %s "$d" | grep -Eq '^[a-z0-9][a-z0-9-]*\.gemsinnovations\.com$' || { echo "Dominio no válido: $d" >&2; exit 1; }
  sudo certbot certonly --nginx -n --agree-tos -m "$email" --keep-until-expiring \
    --deploy-hook "systemctl reload nginx" --cert-name "$d" -d "$d"
  sed "s/__DOMAIN__/$d/g" "$here/host-nginx/institucion.conf.template" |
    sudo tee "/etc/nginx/sites-available/gems-lms-inst-$d" >/dev/null
  sudo ln -sf "/etc/nginx/sites-available/gems-lms-inst-$d" "/etc/nginx/sites-enabled/gems-lms-inst-$d"
done

# 5) Sitio del LMS; se valida antes de recargar para no afectar a los otros sitios
sudo cp "$here/host-nginx/gems-lms.conf" /etc/nginx/sites-available/gems-lms
sudo ln -sf /etc/nginx/sites-available/gems-lms /etc/nginx/sites-enabled/gems-lms
# Bloque comodín de instituciones: solo con su certificado presente (si faltara, nginx -t fallaría para toda la VM)
if sudo test -f /etc/letsencrypt/live/wildcard.gemsinnovations.com/fullchain.pem; then
  sudo cp "$here/host-nginx/gems-lms-instituciones.conf" /etc/nginx/sites-available/gems-lms-instituciones
  sudo ln -sf /etc/nginx/sites-available/gems-lms-instituciones /etc/nginx/sites-enabled/gems-lms-instituciones
  echo "Subdominios de instituciones por comodín activados."
fi
sudo nginx -t
sudo systemctl reload nginx

cat <<MSG

Listo. Falta:
  1. Crear $HOME/app/.env desde .env.production.example y completar cada CHANGE_ME.
  2. Configurar en GitHub los secrets VM_HOST, VM_USER y VM_SSH_KEY del Environment pdn
     (ver docs/deployment.md) y hacer push a main en los dos repos.
MSG
