package com.cbns.imagenes.service.impl;

import java.io.File;
import java.io.InputStream;
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

//    private final String UPLOAD_DIR = "B:\\ShareFolder\\Imagenes\\"; 
    private final String UPLOAD_DIR = "C:\\Users\\Christian Neri\\Pictures\\Respaldo_fotos\\"; 

	@Override
	public ResponseEntity<String> procesarCargaImagen(MultipartFile file, String modifiedDate, String owner) {

		final Logger logger = Logger.getLogger(CargaFileMediaServiceImpl.class.getName());
		
		if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("No se recibió ningún archivo.");
        }

        if (!DirectorioUtil.validaDirectorio(UPLOAD_DIR + owner)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
        
        String nombre 			= file.getOriginalFilename();
        String fechaCliente 	= modifiedDate;
        String fechaResultado	= "No disponible";
        
        try (InputStream inputStream = file.getInputStream()) {
        	// 1. Leer metadatos desde el InputStream
			Metadata metadata = ImageMetadataReader.readMetadata(inputStream);
			// 2. Intentar obtener fecha EXIF
			ExifSubIFDDirectory exif = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
			
			if (exif != null && exif.containsTag(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)) {
                fechaResultado = FechaUtil.normalizarFecha(exif.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL));
				logger.info("[FechaOriginal]");
            } else {
                fechaResultado = FechaUtil.normalizarFecha(fechaCliente);
				logger.info("[FechaModificación]");
            }

            String 	extension 	= obtenerExtension(nombre);
            File 	destino 	= new File(UPLOAD_DIR + owner, fechaResultado + extension);
            
			if (!destino.exists()) {
				file.transferTo(destino);
				logger.info("[SUCCESS] - Renombrado "+ nombre +" → " + fechaResultado + extension);
			}else {
                int contador = 1;
                File nuevoArchivoConNumero;
                String nuevoNombreConNumero = "";
                do {
                    nuevoNombreConNumero = fechaResultado + "-" + contador + extension;
                    nuevoArchivoConNumero = new File(UPLOAD_DIR + owner, nuevoNombreConNumero);
                    contador++;
                } while (nuevoArchivoConNumero.exists());
				logger.info("[SUCCESS] - Renombrado "+ nombre +" → " + nuevoNombreConNumero);
				file.transferTo(nuevoArchivoConNumero);
			}
            
        }catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        return ResponseEntity.ok("Imagen cargada exitosamente.");
	}


    @Override
    public String obtenerIpLocal() {
        try {
            java.util.Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp()) continue;
                java.util.Enumeration<java.net.InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    java.net.InetAddress addr = addresses.nextElement();
                    if (!addr.isLoopbackAddress() && addr.isSiteLocalAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "127.0.0.1"; // Fallback
    }

    public String obtenerExtension(String filename) {
        int punto = filename.lastIndexOf('.');
        return (punto > 0) ? filename.substring(punto) : "";
    }
	
}
