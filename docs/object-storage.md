# Almacenamiento de archivos

`ms-education` admite disco local y S3 sin cambiar los endpoints ni las URLs guardadas en la base.
El modo local sigue siendo el predeterminado.

## S3 o compatible

Configurar en el entorno del servicio:

```env
FILES_STORAGE=s3
S3_BUCKET=gems-lms-produccion
S3_REGION=us-east-1
S3_PREFIX=uploads
AWS_ACCESS_KEY_ID=...
AWS_SECRET_ACCESS_KEY=...
```

En AWS se recomienda usar un rol de instancia o tarea y omitir las claves estáticas. El SDK utiliza
la cadena estándar de credenciales, por lo que también admite `AWS_SESSION_TOKEN` y proveedores de
identidad del entorno.

Para MinIO u otro proveedor compatible:

```env
S3_ENDPOINT=https://objetos.example.com
S3_PATH_STYLE=true
```

El bucket debe ser privado. Los archivos públicos siguen pasando por
`/api/v1/files/public/{id}` para aplicar metadatos, caché y la misma política de acceso. El rol o
usuario necesita `s3:GetObject`, `s3:PutObject` y `s3:DeleteObject` sobre el prefijo configurado.

Antes del cambio en producción, migrar los objetos del volumen local conservando como nombre cada
UUID, validar imágenes y descargas privadas, y conservar una copia del volumen hasta completar la
verificación.
