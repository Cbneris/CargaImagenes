# Documentación del proyecto CargaImagenes

## 1. Propósito

`CargaImagenes` es una API REST desarrollada con Spring Boot para recibir imágenes y guardarlas en un volumen organizado por usuario y año. El nombre del archivo se determina usando primero la fecha original de la imagen (EXIF) y, si no está disponible, la fecha de modificación que envía el cliente.

La estructura de almacenamiento es:

```text
/volume1/homes/{usuario}/Photos/{año}/{fecha}.{extensión}
```

Por ejemplo:

```text
/volume1/homes/christian_neri/Photos/2026/2026-05-24_12.00.00.jpg
```

La raíz `/volume1/homes` se puede cambiar mediante la propiedad `upload.dir` o la variable de entorno `UPLOAD_DIR`. Si no se configura, se usa `/volume1/homes`.

## 2. Componentes del proyecto

- [`CargaImagenesApplication.java`](src/main/java/com/cbns/imagenes/CargaImagenesApplication.java): punto de entrada de Spring Boot.
- [`UploadImagesController.java`](src/main/java/com/cbns/imagenes/controller/UploadImagesController.java): endpoint HTTP para recibir imágenes.
- [`CargaFileMediaService.java`](src/main/java/com/cbns/imagenes/service/CargaFileMediaService.java): contrato del servicio de carga.
- [`CargaFileMediaServiceImpl.java`](src/main/java/com/cbns/imagenes/service/impl/CargaFileMediaServiceImpl.java): procesamiento, lectura de fechas y almacenamiento de imágenes.
- [`FechaUtil.java`](src/main/java/com/cbns/imagenes/util/FechaUtil.java): normalización de fechas EXIF y fechas ISO.
- [`DirectorioUtil.java`](src/main/java/com/cbns/imagenes/util/DirectorioUtil.java): creación de directorios.
- `src/main/resources/static/index.html`, `css/styles.css` y `js/app.js`: estructura separada de la interfaz web, estilos y lógica JavaScript.
- [`application.properties`](src/main/resources/application.properties): nombre de la aplicación, logging y límites de archivos.
- [`docker-compose.yml`](docker-compose.yml) y [`Dockerfile`](Dockerfile): configuración para ejecutar la aplicación en Docker.

## 3. Flujo de carga

1. El cliente envía una imagen y los campos `modifiedDate` y `owner` al endpoint `POST /cargaImagenes/upload`.
2. El controlador entrega los datos a `CargaFileMediaService`.
3. El servicio rechaza archivos vacíos y nombres de usuario no válidos.
4. El servicio intenta leer los metadatos de la imagen y determina la fecha que se usará.
5. La fecha se normaliza al formato `yyyy-MM-dd_HH.mm.ss`.
6. El año se extrae de la fecha y se construye la ruta `{raíz}/{usuario}/Photos/{año}`.
7. El directorio se crea si no existe.
8. Si el destino ya existe, la carga se rechaza con `409 Conflict`; el archivo se agrega a la lista de problemas y no se reintenta.

## 4. Endpoint HTTP

### Interfaz web

Abre `http://localhost:9093/` al ejecutar con Docker Compose, o `http://localhost:8080/` al ejecutar Spring Boot directamente. Allí puedes seleccionar varias imágenes o videos, ver la lista de archivos pendientes y cargarlos secuencialmente. La interfaz muestra el porcentaje de avance y retira de la lista cada archivo que termina de cargar correctamente. Los archivos que fallen pasan a una lista de problemas y se reintentan hasta dos veces más, después de completar la carga inicial de los demás archivos. Los que sigan fallando quedan identificados como cancelados. Por seguridad, el navegador no expone la ruta absoluta local; se muestra la ruta relativa del archivo o su nombre.

El selector de usuario obtiene las carpetas disponibles desde la raíz configurada por `upload.dir` o `UPLOAD_DIR` (por defecto, `/volume1/homes`). El destino se organiza bajo `{raíz}/{usuario}/Photos/{año}`.

### `GET /cargaImagenes/owners`

Devuelve los nombres de las carpetas de usuario encontradas directamente bajo la raíz de almacenamiento. La interfaz usa estos valores para llenar el selector de usuario.

### `POST /cargaImagenes/upload`

El endpoint recibe una solicitud `multipart/form-data` con estos campos:

| Campo | Tipo | Descripción |
|---|---|---|
| `images` | Archivo | Imagen que se va a almacenar. |
| `modifiedDate` | Texto | Fecha proporcionada por el cliente; se usa cuando la imagen no contiene fecha EXIF original. |
| `owner` | Texto | Nombre de usuario que determina el directorio de destino. |

La interfaz envía un archivo por solicitud e itera la lista seleccionada. Para imágenes se conserva la prioridad de fecha EXIF; para videos se utiliza la fecha de modificación del archivo enviada por el navegador.

Si el archivo de destino ya existe, el endpoint responde `409 Conflict` con un mensaje indicando que no se cargó. La interfaz lo conserva en la lista de problemas y lo omite en los reintentos.

### `POST /cargaImagenes/upload/complete`

La interfaz invoca este endpoint al terminar el lote, después de los reintentos. Recibe `owner` y `uploadedCount` y registra un resumen con el propietario y la cantidad total de archivos cargados correctamente.

Ejemplo con `curl`:

Con Docker Compose, usa el puerto `9093`; al ejecutar Spring Boot directamente, usa `8080`.

```bash
curl -X POST http://localhost:9093/cargaImagenes/upload \
  -F "images=@foto.jpg" \
  -F "modifiedDate=2026-05-24T12:00:00Z" \
  -F "owner=christian_neri"
```

Respuestas principales:

- `200 OK`: imagen almacenada; cuerpo `Imagen cargada exitosamente.`
- `400 Bad Request`: el archivo está vacío o el usuario no es válido.
- `409 Conflict`: ya existe un archivo con el mismo nombre de destino; no se sobrescribe ni se genera un nombre alternativo.
- `500 Internal Server Error`: error al leer metadatos, normalizar la fecha, crear el directorio o escribir el archivo. El detalle se registra en el log; el cuerpo de esta respuesta es `null`.

El controlador permite solicitudes CORS desde `http://192.168.1.13:5174`.

## 5. Funcionamiento método por método

### `CargaImagenesApplication`

#### `main(String[] args)`

Arranca la aplicación Spring Boot mediante `SpringApplication.run`. Al iniciar, Spring detecta el controlador y el servicio y configura la API.

### `UploadImagesController`

#### `cargaImagenes(MultipartFile file, String modifiedDate, String owner)`

Está asociado a `POST /cargaImagenes/upload`. Vincula el campo `images` con el archivo y recibe `modifiedDate` y `owner` como parámetros de formulario. Delega el procesamiento a `procesarCargaImagen` y devuelve la respuesta de ese servicio.

### `CargaFileMediaService`

Define las operaciones que expone el servicio:

- `procesarCargaImagen(...)`: procesa y almacena una imagen.
- `obtenerIpLocal()`: obtiene una dirección IP local del equipo.

### `CargaFileMediaServiceImpl`

#### `procesarCargaImagen(MultipartFile file, String modifiedDate, String owner)`

Coordina una carga completa. Primero rechaza un archivo vacío y valida el usuario. Después abre el contenido, llama a `resolverFecha`, obtiene la extensión original y llama a `guardarArchivo`. Si ocurre una excepción durante el procesamiento, registra el error y responde `500`; en caso de éxito responde `200`.

#### `resolverFecha(InputStream inputStream, String fechaCliente)`

Lee los metadatos de la imagen con `ImageMetadataReader`. Si encuentra el directorio EXIF `ExifSubIFDDirectory` con `TAG_DATETIME_ORIGINAL`, usa esa fecha. Si el directorio o el campo no están disponibles, usa la fecha recibida del cliente. En ambos casos llama a `FechaUtil.normalizarFecha`.

La fecha EXIF original tiene prioridad sobre `modifiedDate`. La lectura de metadatos debe poder interpretar el archivo; si falla, la carga termina como error en lugar de guardar silenciosamente una fecha alternativa.

#### `guardarArchivo(MultipartFile file, String owner, String fechaResultado, String extension)`

Extrae el año de `fechaResultado`, solicita la ruta anual a `obtenerDirectorioAnio` y crea los directorios que falten con `DirectorioUtil.validaDirectorio`. Guarda el archivo con la fecha como nombre base sin sobrescribir destinos existentes; un duplicado produce `409 Conflict`. La ruta base `{raíz}/{usuario}/Photos` se registra una sola vez por usuario cuando inicia su primera carga. El log de cada archivo incluye el usuario entre corchetes antes de indicar el renombrado.

La extensión del archivo original se conserva. Por ejemplo, una foto `foto.jpg` queda con extensión `.jpg`.

#### `obtenerDirectorioAnio(String owner, String anio)`

Construye la ruta de destino combinando la raíz configurada, el usuario, `Photos` y el año:

```text
{upload.dir}/{owner}/Photos/{anio}
```

Normaliza la ruta y verifica que siga dentro de la raíz configurada. Si la ruta normalizada escapa de esa raíz, lanza `IOException`.

#### `esPropietarioValido(String owner)`

Acepta nombres de usuario formados únicamente por letras ASCII, números, punto, guion y guion bajo (`[A-Za-z0-9._-]+`). También rechaza los nombres `.` y `..`. Esto evita que el parámetro del usuario introduzca separadores de directorio o rutas relativas.

#### `obtenerAnio(String fechaResultado)`

Exige el formato normalizado `yyyy-MM-dd_HH.mm.ss` y devuelve sus primeros cuatro caracteres como año. Si la fecha es nula o no coincide con ese formato, lanza `IllegalArgumentException`.

#### `generarNombreDisponible(File directorioAnio, String fechaResultado, String extension)`

Busca el primer nombre de archivo que no exista, agregando un sufijo numérico antes de la extensión. Se conserva en el código, pero no se usa en las cargas porque crear nombres alternativos podría guardar duplicados:

```text
2026-05-24_12.00.00.jpg
2026-05-24_12.00.00-1.jpg
2026-05-24_12.00.00-2.jpg
```

La búsqueda se realizaría dentro de la carpeta del usuario y año. Actualmente, si ya existe el destino con la misma extensión, la carga se rechaza en lugar de llamar este método.

#### `obtenerIpLocal()`

Llama a `buscarIpSiteLocal`. Si ocurre un error, registra la excepción y devuelve `127.0.0.1` como valor alternativo.

#### `buscarIpSiteLocal()`

Recorre las interfaces de red del equipo. Omite las interfaces que están apagadas o que son loopback, y devuelve la primera dirección IP local que encuentra. Si no encuentra ninguna, devuelve `127.0.0.1`.

#### `buscarPrimeraIpValida(NetworkInterface ni)`

Examina las direcciones de una interfaz y devuelve la primera que no sea loopback y que sea una dirección local (`isSiteLocalAddress`). Si no encuentra ninguna, devuelve `null`.

#### `obtenerExtension(String filename)`

Devuelve desde el último punto hasta el final del nombre, incluida la extensión. Si el nombre no tiene extensión, devuelve una cadena vacía.

### `FechaUtil`

#### `normalizarFecha(String fecha)`

Convierte las fechas reconocidas al formato común que se utiliza en los nombres y directorios:

```text
yyyy-MM-dd_HH.mm.ss
```

Formatos contemplados por la implementación:

1. **Fecha ya normalizada**, por ejemplo `2026-05-24_12.00.00`: se devuelve tal cual.
2. **Fecha EXIF**, por ejemplo `2026:05:24 12:00:00`: se transforma a `2026-05-24_12.00.00`.
3. **Fecha ISO tipo `Instant`**, por ejemplo `2026-05-24T12:00:00Z`: se interpreta como instante y se convierte a la zona horaria del sistema donde corre la aplicación antes de formatearla.

Para una fecha ISO, el cambio de zona horaria puede afectar la hora o incluso el día y el año locales. Por ello, el año de la carpeta corresponde al año de la fecha normalizada final.

Si el parseo EXIF o ISO falla, el método devuelve un texto de error; si el formato no coincide, devuelve un texto de formato desconocido. Esos textos no cumplen el formato normalizado, por lo que `obtenerAnio` rechaza la fecha y la carga se reporta como `500`.

### `DirectorioUtil`

#### `validaDirectorio(String ruta)`

Comprueba si la ruta ya existe. Si no existe, intenta crearla junto con sus directorios padre mediante `mkdirs`. Devuelve `true` si ya existía o se pudo crear, y `false` si no.

## 6. Fechas y nombres de archivo: ejemplos

Si la imagen contiene `DateTimeOriginal` EXIF:

```text
EXIF: 2026:05:24 12:00:00
```

esa fecha prevalece sobre la fecha del formulario. El nombre base resultante será:

```text
2026-05-24_12.00.00
```

Con `owner=christian_neri` y `UPLOAD_DIR=/volume1/homes`, el destino es:

```text
/volume1/homes/christian_neri/Photos/2026/2026-05-24_12.00.00.jpg
```

Si no hay fecha EXIF original, se procesa `modifiedDate`. La fecha ISO se convierte a la zona horaria del sistema antes de obtener el año y el nombre.

Si el nombre ya está ocupado, la carga se rechaza; el archivo existente no se sobrescribe y el archivo entrante no se reintenta.

## 7. Configuración de almacenamiento y Docker

### Variables y propiedades

- `UPLOAD_DIR`: directorio raíz de los homes. Por defecto es `/volume1/homes`.
- `LOGGING_FILE_NAME`: ruta del archivo de log configurada en Docker y en `application.properties`: `/volume1/homes/admin/uploadImages/carga-imagenes.log`.
- `spring.servlet.multipart.max-file-size` y `spring.servlet.multipart.max-request-size`: límites configurados en `application.properties` a `4096MB`.

En Docker Compose, el volumen del NAS se monta en `/volume1/homes`, y se declara `UPLOAD_DIR=/volume1/homes`. Asegura que la ruta de origen exista en el host del contenedor y que el proceso tenga permiso de escritura. Para los detalles del despliegue, consulta [`DOCKER_CONFIG.md`](DOCKER_CONFIG.md).

### Construcción y pruebas

Construir la imagen Docker desde la raíz:

```bash
docker compose build
```

Iniciar el servicio:

```bash
docker compose up -d
```

Ejecutar las pruebas Java:

```bash
mvn test
```

El proyecto utiliza Java 17 y Maven. Las dependencias principales incluyen Spring Boot Web para la API y Metadata Extractor para la lectura de metadatos de imagen.

## 8. Pruebas existentes

`CargaFileMediaServiceImplTest` verifica la obtención de IP, la extracción de extensiones, la extracción del año, la ruta por usuario/año, el rechazo de rutas fuera del volumen, los rechazos de archivos vacíos, usuarios inválidos y destinos duplicados sin sobrescribir el archivo existente. La prueba de contexto verifica que Spring Boot pueda iniciar el contexto de la aplicación.

## 9. Historial de cambios

### 2026-11-22

- Se agregó una interfaz web para seleccionar varias imágenes y videos, mantenerlos en una lista y cargarlos secuencialmente con indicador de progreso y reintentos automáticos para archivos fallidos.
- Se separaron la página, los estilos y la lógica del navegador en `static/index.html`, `static/css/styles.css` y `static/js/app.js`.
- Se añadió el porcentaje de avance; los archivos cargados se retiran de la lista y los fallidos se reintentan hasta dos veces después de completar la carga inicial.
- Se agregó al log la ruta `{raíz}/{usuario}/Photos` una sola vez por usuario, el usuario en los mensajes de renombrado y un resumen final por lote con la cantidad cargada.
- Se deshabilitó el uso de nombres alternativos para duplicados: el servidor responde `409 Conflict`, la interfaz lista el archivo y no lo reintenta. Se conserva `generarNombreDisponible` sin llamadas.
- Se agregó `GET /cargaImagenes/owners` para poblar el selector con las carpetas de usuario existentes bajo la raíz de almacenamiento configurada.
- Se habilitó el uso de la fecha de modificación del archivo para videos; las imágenes conservan la prioridad de fecha EXIF.
- Se documentó que el navegador no expone la ruta absoluta local; la interfaz conserva los archivos seleccionados y muestra su nombre o ruta relativa.
- Se validó el cambio ejecutando `mvn test` (14 pruebas aprobadas).
