# Home personalizada por institución

Cada institución puede escribir el HTML y el CSS de su portada. Se publica en su propio subdominio
(`<slug>.gemsinnovations.com`). **No se permite JavaScript.**

## Modelo (ms-admin)

| Tabla | Contenido |
|---|---|
| `institution_home` | Una fila por institución: `slug` (único), borrador (`draft_html`, `draft_css`, quién y cuándo) y `published_version` |
| `institution_home_versions` | Historial: cada publicación crea la versión `n+1` (`html`, `css`, `note`, `published_by`, `published_at`) |

Las dos tienen `ON DELETE CASCADE` con `institutions`.

## API

| Método | Ruta | Quién |
|---|---|---|
| GET | `/api/v1/home/{id}` | admin de la institución, super admin |
| PUT | `/api/v1/home/{id}/draft` `{html, css}` | idem (se sanea al guardar) |
| POST | `/api/v1/home/{id}/publish` `{note?}` | idem: borrador → nueva versión publicada |
| DELETE | `/api/v1/home/{id}/publish` | idem: deja de mostrarse (el historial se conserva) |
| GET | `/api/v1/home/{id}/versions`, `/versions/{n}` | idem |
| POST | `/api/v1/home/{id}/versions/{n}/restore` | idem: copia la versión al borrador |
| PUT | `/api/v1/home/{id}/slug` `{slug}` | **solo super admin** (3-40 caracteres `a-z0-9-`, único, sin palabras reservadas) |
| GET | `/api/v1/public/home/{slug}` | público: marca + versión publicada (404 si el slug no existe) |

## Saneado

- HTML (`jsoup` Safelist): sin `<script>`, `<form>`, `on*=`, `javascript:` ni `style=`. Se permiten enlaces relativos.
  El front lo vuelve a sanear con Angular antes de pintarlo.
- CSS: sin `@import`, `expression()`, `behavior`, escapes `\`; `url()` solo `https:`, relativas o `data:image/`.
  El front antepone `.tenant-home-N` a cada selector (`body`/`html`/`:root` pasan a ser el contenedor), así que
  no afecta al resto de la app.
- Límites: 200 000 caracteres de HTML y 100 000 de CSS.

## Subdominios

1. **DNS**: registro `*.gemsinnovations.com` hacia la VM.
2. **Certificado comodín** (requiere DNS-01):
   `certbot certonly --manual --preferred-challenges dns -d "*.gemsinnovations.com" -d gemsinnovations.com`
3. **nginx** (en `ops/host-nginx/gems-lms.conf`), conservando el `Host`:

```nginx
server {
    listen 443 ssl http2;
    server_name ~^(?<institution>[a-z0-9-]+)\.gemsinnovations\.com$;
    ssl_certificate     /etc/letsencrypt/live/gemsinnovations.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/gemsinnovations.com/privkey.pem;
    location / {
        proxy_pass http://gems_lms_web;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

   Declárala después de los `server` con nombre exacto (`lms.`, `api.*`): nginx da prioridad a los nombres exactos.
4. **Front (SSR)**: `NG_ALLOWED_HOSTS=lms.gemsinnovations.com,*.gemsinnovations.com` y
   `TENANT_BASE_DOMAIN=gemsinnovations.com`.
5. **CORS** del gateway: añadir `https://*.gemsinnovations.com` a `CORS_ALLOWED_ORIGINS` (usa patrones de origen).

Cada subdominio es un origen distinto: la sesión del navegador no se comparte entre `unal.` y `lms.`.
En desarrollo sirve `http://unal.localhost:4200`.

## Modos

Cada borrador y cada versión llevan un `mode`:

| Modo | Qué se ve |
|---|---|
| `BASE` (por defecto) | Cabecera, portada y tarjetas de cursos de la institución con su marca. Su CSS la restiliza; su HTML, si lo hay, es un banner bajo la cabecera. Sin nada publicado se ve esta landing sin personalizar. |
| `CUSTOM` | Su HTML reemplaza la landing completa. Marcadores: `{{cursos}}`, `{{nombre}}`, `{{logo}}`, `{{ingresar}}`. |

Clases estables para el CSS: `.tb-header .tb-brand .tb-logo .tb-name .tb-login .tb-hero .tb-cta .tb-courses .tb-footer`
y las tarjetas `.gems-cursos .gems-curso .gems-curso__img .gems-curso__body .gems-curso__title .gems-curso__desc .gems-curso__meta`.
Los estilos por defecto usan `:where()` (especificidad 0), así que cualquier regla de la institución los supera.

Los cursos salen de `GET /api/v1/public/institutions/{id}/courses` (ms-education, público): solo título, resumen e instructor
de los cursos **publicados**; las lecciones siguen requiriendo sesión. Si una institución no quiere exponer ni eso, hay que
añadir un indicador por curso antes de abrir este endpoint en producción.

## Cursos que muestra la landing de una institución

La landing de una institución lista **solo los cursos públicos que se le asignaron**, vía
`GET /api/v1/public/institutions/{institutionId}/courses`. Los cursos públicos (gratis, sin registro) viven en el
catálogo abierto (`gems-abierto`), que es lo que permite estudiarlos como invitado; la columna
`courses.showcase_institution_id` dice qué institución muestra cada uno. Los cursos privados de una institución
nunca se listan, y el catálogo abierto de GEMS no cambia.

El super admin asigna (o quita) un curso abierto:

```
PUT /api/v1/courses/{courseId}/showcase   {"institutionId":"inst-3"}    # null o "" lo quita
```

Ejemplo de desarrollo: «Minecraft: ¿cuánto sabes?» (`seed/open-bloques.mjs`) → `inst-3` (Knowledge Craft).
Tras ejecutar ese seed hay que repetir la asignación.
