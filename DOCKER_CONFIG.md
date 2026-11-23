# Configuración Docker — CargaImagenes

Este archivo contiene instrucciones para construir y ejecutar la imagen Docker de la aplicación Spring Boot, usando el volumen del NAS para almacenar las imágenes por usuario y los logs.

## Archivos relevantes

- `Dockerfile` (multi-stage) — compila con Maven y genera `app.jar`.
- `.dockerignore` — evita copiar carpetas innecesarias.

## Construir la imagen

Desde la raíz del proyecto:

```bash
docker build -t carga-imagenes:1.0.0 .
```

## Ejecutar el contenedor con volumen del NAS

Ejemplo que monta `/volume1/homes` del NAS en la misma ruta dentro del contenedor:

```bash
docker run -d -p 9093:8080 -v "/volume1/homes:/volume1/homes" -e UPLOAD_DIR=/volume1/homes --name carga-imagenes carga-imagenes:1.0.0
```

Las imágenes de cada usuario se guardan en `/volume1/homes/{usuario}/Photos/{año}`. Por ejemplo, las imágenes de `christian_neri` tomadas en 2026 se guardan en `/volume1/homes/christian_neri/Photos/2026`.

La variable de entorno `UPLOAD_DIR` define la raíz de los homes y tiene como valor predeterminado `/volume1/homes`.

Los logs se escriben en `/volume1/homes/admin/uploadImages/carga-imagenes.log`. El mismo bind mount del volumen del NAS persiste ese archivo fuera del contenedor. Asegura que la ruta exista o que el proceso tenga permisos para crearla y escribir en ella.

## Ejemplo `docker-compose.yml`

El archivo incluido monta el volumen del NAS para las imágenes y para el archivo de log:

```yaml
services:
  carga-imagenes:
    image: carga-imagenes:1.0.0
    build: .
    ports:
      - "9093:8080"
    environment:
      - UPLOAD_DIR=/volume1/homes
      - LOGGING_FILE_NAME=/volume1/homes/admin/uploadImages/carga-imagenes.log
    volumes:
      - "/volume1/homes:/volume1/homes"
    restart: unless-stopped
```

## Buenas prácticas

- Asegura permisos de escritura en `/volume1/homes` para el usuario que ejecuta el contenedor.
- Evita montar directorios con archivos sensibles sin controles de acceso.
- Para producción, considera usar un volumen gestionado o almacenamiento en red compartido.
