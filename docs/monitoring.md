# Monitoreo

Los cuatro servicios publican métricas Prometheus en `/actuator/prometheus` y etiquetan las series
con `application`. El proxy Nginx responde 404 para esa ruta, por lo que el recolector debe acceder
por la red privada de Docker. Salud básica continúa disponible en `/actuator/health`.

## Iniciar Prometheus y Grafana

Definir una contraseña exclusiva en `.env`:

```env
GRAFANA_ADMIN_PASSWORD=una-clave-larga-y-exclusiva
```

Después de iniciar el despliegue principal:

```bash
docker compose -f docker-compose.yml -f observability/docker-compose.monitoring.yml up -d
```

Prometheus queda en `127.0.0.1:9090` y Grafana en `127.0.0.1:3001`. Los puertos están ligados a
localhost; para administración remota se recomienda un túnel SSH o un proxy con autenticación. La
fuente Prometheus se configura automáticamente.

## Alertas mínimas recomendadas

- Servicio sin responder durante más de dos minutos (`up == 0`).
- Tasa sostenida de respuestas 5xx.
- Latencia p95 elevada en peticiones HTTP.
- Uso de memoria JVM cercano al máximo y pausas de GC crecientes.
- Pocas conexiones disponibles en PostgreSQL o errores de Redis.
- Espacio de disco bajo para bases, uploads, Prometheus y respaldos.
- Última copia válida fuera de la ventana esperada.

La entrega de alertas (correo, Slack, PagerDuty u otro destino) se configura al desplegar, porque
requiere las credenciales y responsables reales de la organización.
