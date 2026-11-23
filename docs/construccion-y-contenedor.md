# Crear la imagen y el contenedor de CargaImagenes

Esta guía explica cómo construir la imagen Docker del proyecto y cómo crear y administrar un contenedor. Los comandos deben ejecutarse desde la raíz del proyecto, donde están `Dockerfile` y `docker-compose.yml`.

## Requisitos

- Docker Engine o Docker Desktop instalado y en ejecución.
- Acceso a la carpeta `/volume1/homes` en el equipo donde se ejecutará el contenedor. El montaje configurado permite guardar allí las imágenes y el archivo de log.

## 1. Construir la imagen

Desde la raíz del proyecto, ejecuta:

```bash
docker build -t carga-imagenes:1.0.0 .
```

Docker usa el `Dockerfile` para construir la aplicación con Maven y Java 17 y luego empaqueta el JAR en una imagen de ejecución Java 17. El punto (`.`) indica que la raíz del proyecto es el contexto de construcción.

Comprueba que la imagen se haya creado:

```bash
docker image ls carga-imagenes
```

## 2. Crear y ejecutar un contenedor

Con la imagen creada, ejecuta este comando en el host que tenga disponible la ruta `/volume1/homes`:

```bash
docker run -d \
  --name carga-imagenes \
  -p 8080:8080 \
  -e UPLOAD_DIR=/volume1/homes \
  -e LOGGING_FILE_NAME=/volume1/homes/admin/uploadImages/carga-imagenes.log \
  -v /volume1/homes:/volume1/homes \
  --restart unless-stopped \
  carga-imagenes:1.0.0
```

El comando crea el contenedor `carga-imagenes` en segundo plano, publica el puerto 8080 y monta la carpeta del host en la misma ruta dentro del contenedor. Asegúrate de que el proceso de Docker tenga permiso para escribir en esa carpeta. Si la ruta de origen no existe en el host, créala antes de iniciar el contenedor.

Comprueba el estado y consulta los logs:

```bash
docker ps
docker logs -f carga-imagenes
```

La API queda disponible en `http://localhost:8080`. Para detener el contenedor, ejecuta `docker stop carga-imagenes`; para volver a iniciarlo, ejecuta `docker start carga-imagenes`.

## Alternativa: construir y ejecutar con Docker Compose

El archivo `docker-compose.yml` ya declara la construcción de la imagen, el puerto, las variables de entorno y el volumen. Desde la raíz del proyecto, ejecuta:

```bash
docker compose up -d --build
```

Comprueba el servicio y sus logs:

```bash
docker compose ps
docker compose logs -f carga-imagenes
```

Para detener y eliminar el contenedor creado por Compose:

```bash
docker compose down
```

Usa Docker Compose **o** el comando `docker run` para iniciar la aplicación, no ambos a la vez: los dos intentan publicar el puerto 8080 y podrían entrar en conflicto.
