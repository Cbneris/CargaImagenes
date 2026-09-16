package com.cbns.imagenes.service.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cbns.imagenes.service.CargaFileMediaService;
import com.cbns.imagenes.util.DirectorioUtil;
import com.cbns.imagenes.util.FechaUtil;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;

@Service
public class CargaFileMediaServiceImpl implements CargaFileMediaService {

    private static final Logger logger = Logger.getLogger(CargaFileMediaServiceImpl.class.getName());

//    private final String UPLOAD_DIR = "B:\\ShareFolder\\Imagenes\\";
    private final String UPLOAD_DIR = "C:\\Users\\Christian Neri\\Pictures\\Respaldo_fotos\\";

    @Override
    public ResponseEntity<String> procesarCargaImagen(MultipartFile file, String modifiedDate, String owner) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("No se recibió ningún archivo.");
        }

        if (!DirectorioUtil.validaDirectorio(UPLOAD_DIR + owner)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        try (InputStream inputStream = file.getInputStream()) {
            String fechaResultado = resolverFecha(inputStream, modifiedDate);
            String extension = obtenerExtension(file.getOriginalFilename());

            guardarArchivo(file, owner, fechaResultado, extension);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error al procesar la carga de imagen", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        return ResponseEntity.ok("Imagen cargada exitosamente.");
    }

    /**
     * Determina la fecha a usar como nombre de archivo: EXIF si existe, si no la del cliente.
     */
    private String resolverFecha(InputStream inputStream, String fechaCliente) throws Exception {
        Metadata metadata = ImageMetadataReader.readMetadata(inputStream);
        ExifSubIFDDirectory exif = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);

        if (exif != null && exif.containsTag(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)) {
            logger.info("[FechaOriginal]");
            return FechaUtil.normalizarFecha(exif.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL));
        }

        logger.info("[FechaModificación]");
        return FechaUtil.normalizarFecha(fechaCliente);
    }

    /**
     * Guarda el archivo con el nombre base, o con un sufijo numérico si ya existe.
     */
    private void guardarArchivo(MultipartFile file, String owner, String fechaResultado, String extension) throws IOException {
        File destino = new File(UPLOAD_DIR + owner, fechaResultado + extension);

        if (!destino.exists()) {
            file.transferTo(destino);
            logger.info("[SUCCESS] - Renombrado " + file.getOriginalFilename() + " → " + destino.getName());
            return;
        }

        File destinoConNumero = generarNombreDisponible(owner, fechaResultado, extension);
        file.transferTo(destinoConNumero);
        logger.info("[SUCCESS] - Renombrado " + file.getOriginalFilename() + " → " + destinoConNumero.getName());
    }

    /**
     * Busca el primer nombre disponible agregando un contador incremental.
     */
    private File generarNombreDisponible(String owner, String fechaResultado, String extension) {
        int contador = 1;
        File candidato;
        do {
            candidato = new File(UPLOAD_DIR + owner, fechaResultado + "-" + contador + extension);
            contador++;
        } while (candidato.exists());
        return candidato;
    }

    @Override
    public String obtenerIpLocal() {
        try {
            return buscarIpSiteLocal();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error al obtener IP local", e);
            return "127.0.0.1";
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
        return "127.0.0.1";
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
        return (punto > 0) ? filename.substring(punto) : "";
    }
}