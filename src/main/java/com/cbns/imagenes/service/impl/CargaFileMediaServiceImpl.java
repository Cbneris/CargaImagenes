package com.cbns.imagenes.service.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cbns.imagenes.constantes.ConstantesTecnicas;
import com.cbns.imagenes.constantes.MensajesSistema;
import com.cbns.imagenes.service.CargaFileMediaService;
import com.cbns.imagenes.util.DirectorioUtil;
import com.cbns.imagenes.util.FechaUtil;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;

@Service
public class CargaFileMediaServiceImpl implements CargaFileMediaService {

    private static final Logger logger = LoggerFactory.getLogger(CargaFileMediaServiceImpl.class);

    @Value(ConstantesTecnicas.PROPIEDAD_DIRECTORIO_UPLOAD)
    private String uploadDir = ConstantesTecnicas.DIRECTORIO_HOMES;
    private final Set<String> ownersWithLoggedStoragePath = ConcurrentHashMap.newKeySet();

    @Override
    public ResponseEntity<String> procesarCargaImagen(MultipartFile file, String modifiedDate, String owner) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(MensajesSistema.ERROR_ARCHIVO_VACIO);
        }

        if (!esPropietarioValido(owner)) {
            return ResponseEntity.badRequest().body(MensajesSistema.ERROR_PROPIETARIO_INVALIDO);
        }

        logStoragePathOnce(owner);

        try (InputStream inputStream = file.getInputStream()) {
            String fechaResultado = resolverFecha(inputStream, modifiedDate, file.getContentType());
            String extension = obtenerExtension(file.getOriginalFilename());

            guardarArchivo(file, owner, fechaResultado, extension);

        } catch (FileAlreadyExistsException e) {
            String nombreArchivo = e.getFile() == null
                    ? file.getOriginalFilename()
                    : Paths.get(e.getFile()).getFileName().toString();
            String mensaje = MensajesSistema.ERROR_ARCHIVO_DUPLICADO.formatted(nombreArchivo);
            logger.warn(MensajesSistema.LOG_ARCHIVO_DUPLICADO,
                    owner, file.getOriginalFilename(), nombreArchivo);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(mensaje);
        } catch (Exception e) {
            logger.error(MensajesSistema.ERROR_PROCESAR_CARGA, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        return ResponseEntity.ok(MensajesSistema.CARGA_EXITOSA);
    }

    @Override
    public ResponseEntity<String> registrarResumenCarga(String owner, int cantidadCargada) {
        if (!esPropietarioValido(owner)) {
            return ResponseEntity.badRequest().body(MensajesSistema.ERROR_PROPIETARIO_INVALIDO);
        }
        if (cantidadCargada < 0) {
            return ResponseEntity.badRequest().body(MensajesSistema.ERROR_CANTIDAD_ARCHIVOS_NEGATIVA);
        }

        logger.info(MensajesSistema.LOG_RESUMEN_CARGA, owner, cantidadCargada);
        return ResponseEntity.ok(MensajesSistema.RESUMEN_CARGA_REGISTRADO);
    }

    private void logStoragePathOnce(String owner) {
        if (ownersWithLoggedStoragePath.add(owner)) {
            Path rutaPropietario = Paths.get(uploadDir).toAbsolutePath().normalize()
                    .resolve(owner).resolve(ConstantesTecnicas.DIRECTORIO_FOTOS);
            logger.info(MensajesSistema.LOG_DIRECTORIO_ALMACENAMIENTO, owner, rutaPropietario);
        }
    }

    @Override
    public List<String> obtenerOwnersDisponibles() throws IOException {
        Path raiz = Paths.get(uploadDir).toAbsolutePath().normalize();
        if (!Files.isDirectory(raiz)) {
            throw new IOException(MensajesSistema.ERROR_DIRECTORIO_USUARIOS.formatted(raiz));
        }

        try (Stream<Path> directorios = Files.list(raiz)) {
            return directorios
                    .filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .filter(this::esPropietarioValido)
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Determina la fecha a usar como nombre de archivo: EXIF si existe, si no la del cliente.
     */
    private String resolverFecha(InputStream inputStream, String fechaCliente, String contentType) throws Exception {
        if (contentType != null && contentType.startsWith(ConstantesTecnicas.PREFIJO_MIME_VIDEO)) {
            logger.info(MensajesSistema.LOG_FECHA_MODIFICACION);
            return FechaUtil.normalizarFecha(fechaCliente);
        }

        Metadata metadata = ImageMetadataReader.readMetadata(inputStream);
        ExifSubIFDDirectory exif = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);

        if (exif != null && exif.containsTag(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)) {
            logger.info(MensajesSistema.LOG_FECHA_ORIGINAL);
            return FechaUtil.normalizarFecha(exif.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL));
        }

        logger.info(MensajesSistema.LOG_FECHA_MODIFICACION);
        return FechaUtil.normalizarFecha(fechaCliente);
    }

    /**
     * Guarda el archivo con el nombre base y rechaza el destino si ya existe.
     */
    private void guardarArchivo(MultipartFile file, String owner, String fechaResultado, String extension) throws IOException {
        String anio = obtenerAnio(fechaResultado);
        File directorioAnio = obtenerDirectorioAnio(owner, anio).toFile();
        if (!DirectorioUtil.validaDirectorio(directorioAnio.getPath())) {
            throw new IOException(MensajesSistema.ERROR_CREAR_DIRECTORIO_ANUAL.formatted(owner, anio));
        }

        File destino = new File(directorioAnio, fechaResultado + extension);

        if (destino.exists()) {
            throw new FileAlreadyExistsException(destino.getAbsolutePath());
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destino.toPath());
        }

        logger.info(MensajesSistema.LOG_ARCHIVO_RENOMBRADO,
                owner, file.getOriginalFilename(), destino.getName());
    }

    Path obtenerDirectorioAnio(String owner, String anio) throws IOException {
        Path raiz = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path rutaAnio = raiz.resolve(owner).resolve(ConstantesTecnicas.DIRECTORIO_FOTOS).resolve(anio).normalize();
        if (!rutaAnio.startsWith(raiz)) {
            throw new IOException(MensajesSistema.ERROR_RUTA_FUERA_VOLUMEN);
        }
        return rutaAnio;
    }

    private boolean esPropietarioValido(String owner) {
        return owner != null && owner.matches(ConstantesTecnicas.PATRON_PROPIETARIO)
                && !owner.equals(ConstantesTecnicas.DIRECTORIO_ACTUAL)
                && !owner.equals(ConstantesTecnicas.DIRECTORIO_PADRE);
    }

    String obtenerAnio(String fechaResultado) {
        if (fechaResultado == null || !fechaResultado.matches(ConstantesTecnicas.PATRON_FECHA_NORMALIZADA)) {
            throw new IllegalArgumentException(
                    MensajesSistema.ERROR_FORMATO_FECHA.formatted(ConstantesTecnicas.FORMATO_FECHA_NORMALIZADA));
        }
        return fechaResultado.substring(0, 4);
    }

    /**
     * Método conservado, pero no debe usarse en cargas para evitar guardar archivos duplicados
     * con nombres alternativos.
     */
    private File generarNombreDisponible(File directorioAnio, String fechaResultado, String extension) {
        int contador = 1;
        File candidato;
        do {
            candidato = new File(directorioAnio,
                    fechaResultado + ConstantesTecnicas.SEPARADOR_SUFIJO_DUPLICADO + contador + extension);
            contador++;
        } while (candidato.exists());
        return candidato;
    }

    @Override
    public String obtenerIpLocal() {
        try {
            return buscarIpSiteLocal();
        } catch (Exception e) {
            logger.error(MensajesSistema.ERROR_OBTENER_IP_LOCAL, e);
            return ConstantesTecnicas.IP_LOCAL_FALLBACK;
        }
    }

    private String buscarIpSiteLocal() throws SocketException {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface ni = interfaces.nextElement();
            if (ni.isLoopback() || !ni.isUp()) continue;

            String ip = buscarPrimeraIpValida(ni);
            if (ip != null) {
                return ip;
            }
        }
        return ConstantesTecnicas.IP_LOCAL_FALLBACK;
    }

    private String buscarPrimeraIpValida(NetworkInterface ni) {
        Enumeration<InetAddress> addresses = ni.getInetAddresses();
        while (addresses.hasMoreElements()) {
            InetAddress addr = addresses.nextElement();
            if (!addr.isLoopbackAddress() && addr.isSiteLocalAddress()) {
                return addr.getHostAddress();
            }
        }
        return null;
    }

    public String obtenerExtension(String filename) {
        int punto = filename.lastIndexOf('.');
        return (punto > 0) ? filename.substring(punto) : ConstantesTecnicas.CADENA_VACIA;
    }
}