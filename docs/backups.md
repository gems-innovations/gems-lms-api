# Copias y restauración

Los respaldos incluyen las tres bases PostgreSQL, los archivos subidos y un manifiesto con
SHA-256. La carpeta `backups/` está excluida de Git porque puede contener datos personales.

## Crear una copia

Con el despliegue completo:

```powershell
.\ops\backup.ps1
```

Con las bases locales y los microservicios ejecutados desde Gradle:

```powershell
.\ops\backup.ps1 -ComposeFile docker-compose-local.yml
```

Copiar el directorio resultante a almacenamiento cifrado y externo al servidor. Automatizar el
comando desde el programador del sistema y conservar varias generaciones. Una copia que permanece
solo en el mismo disco no protege ante pérdida del host.

## Restaurar

Detener primero el tráfico y los microservicios. La restauración valida todas las sumas antes de
modificar datos y exige el interruptor explícito:

```powershell
.\ops\restore.ps1 -BackupPath .\backups\20261004-030000Z -ConfirmRestore
```

Para el entorno local, agregar `-ComposeFile docker-compose-local.yml`. Al terminar, iniciar los
servicios, ejecutar `check-health.sh` y probar acceso, descarga de archivos y un curso.

En producción se recomienda una copia diaria, retención diaria/semanal/mensual según la política de
la organización, cifrado, acceso restringido y una restauración de prueba periódica en un entorno
aislado.
