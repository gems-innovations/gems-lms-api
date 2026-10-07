#!/bin/sh
# Preparación única de la VM de Oracle (Ubuntu). Ejecutar como el usuario de despliegue:
#   sh setup-vm.sh lms.gemsinnovations.com api.auth.gemsinnovations.com tu@correo.com
# Requisitos previos: los dominios ya apuntan a la IP pública de la VM y la Security List /
# NSG de Oracle permite entrada TCP 22, 80 y 443.
set -eu

[ $# -ge 3 ] || { echo "Uso: $0 <dominio-front> <dominio-api> <correo-certbot>" >&2; exit 1; }
web_domain=$1; api_domain=$2; email=$3

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
command -v netfilter-persistent >/dev/null 2>&1 && sudo netfilter-persistent save || true

# 3) Carpetas que monta el compose
mkdir -p "$HOME/app"
sudo mkdir -p /var/www/certbot

# 4) Certificados Let's Encrypt (modo standalone: el puerto 80 debe estar libre)
for domain in "$web_domain" "$api_domain"; do
  sudo docker run --rm -p 80:80 \
    -v /etc/letsencrypt:/etc/letsencrypt -v /var/www/certbot:/var/www/certbot \
    certbot/certbot certonly --standalone -d "$domain" -m "$email" --agree-tos --no-eff-email -n
done

# 5) Renovación automática (certbot webroot, con nginx ya corriendo)
cron="0 3 * * 1 docker run --rm -v /etc/letsencrypt:/etc/letsencrypt -v /var/www/certbot:/var/www/certbot certbot/certbot renew -q --webroot -w /var/www/certbot && docker exec gems-nginx nginx -s reload"
( crontab -l 2>/dev/null | grep -v 'certbot/certbot renew'; echo "$cron" ) | crontab -

cat <<MSG

Listo. Falta:
  1. Copiar .env.production.example a $HOME/app/.env y completar cada CHANGE_ME
     (incluye API_BASE_URL=https://$api_domain/api/v1 y FRONTEND_URL=https://$web_domain).
  2. Configurar en GitHub los secrets/variables (ver docs/deployment.md) y hacer push a main.
MSG
