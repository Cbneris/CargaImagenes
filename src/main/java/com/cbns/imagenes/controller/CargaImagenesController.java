package com.cbns.imagenes.controller;

import java.io.File;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;

@RestController
@RequestMapping("/imagenes")
@CrossOrigin(origins = "http://192.168.1.10:5173") // Permite llamadas desde React (puerto Vite)
public class CargaImagenesController {

	private final Logger logger = Logger.getLogger(CargaImagenesController.class.getName());

//    private final String UPLOAD_DIR = "B:\\ShareFolder\\Imagenes\\IMG_LOADS_2"; 
	private final String UPLOAD_DIR = "C:\\Users\\Christian Neri\\Pictures\\Respaldo_fotos"; 
	
	@PostMapping("/upload")
	public ResponseEntity<String> cargaImagenes(
			@RequestParam("images") MultipartFile file,
		    @RequestParam("modifiedDate") String modifiedDate){
				
		if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("No se recibió ningún archivo.");
        }
		
		if (!validaDirectorio()) {
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
	    }
	    
	    List<Map<String, String>> respuesta = new ArrayList<>();
	    
		String nombre 			= file.getOriginalFilename();
		String fechaResultado 	= "No disponible";
		String fechaCliente 	= modifiedDate;

		System.out.println("Nombre: " + nombre);
		System.out.println("Fecha enviada por cliente: " + fechaCliente);

		if (!file.isEmpty()) {
			try (InputStream inputStream = file.getInputStream()) {

				// 1. Leer metadatos desde el InputStream
				Metadata metadata = ImageMetadataReader.readMetadata(inputStream);

				// 2. Intentar obtener fecha EXIF
				ExifSubIFDDirectory exif = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
				if (exif != null && exif.containsTag(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)) {
					fechaResultado = exif.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
//					fechaResultado = convertirIsoAFormatoPersonalizado(fechaResultado);
					fechaResultado = normalizarFecha(fechaResultado);
					System.out.println("Fecha EXIF original: " + fechaResultado);
				} else {
					// 3. Si no hay EXIF, usar la fecha modificada enviada por el cliente
					//fechaResultado = convertirIsoAFormatoPersonalizado(fechaCliente);
					fechaResultado = normalizarFecha(fechaCliente);
					System.out.println("Usando fecha del cliente (fallback): " + fechaResultado);
				}

				String extension = obtenerExtension(nombre);

				// 4. Guardar archivo
				File destino = new File(UPLOAD_DIR, fechaResultado + extension);
				if (!destino.exists()) {
					file.transferTo(destino);
					logger.info("[SUCCESS] - Renombrado "+ nombre +" → " + fechaResultado + extension);
				}else {
                    int contador = 1;
                    File nuevoArchivoConNumero;
                    String nuevoNombreConNumero = "";
                    do {
                        nuevoNombreConNumero = fechaResultado + "-" + contador + extension;
                        nuevoArchivoConNumero = new File(UPLOAD_DIR, nuevoNombreConNumero);
                        contador++;
                    } while (nuevoArchivoConNumero.exists());
					file.transferTo(nuevoArchivoConNumero);
					logger.info("[SUCCESS] - Renombrado "+ nombre +" → " + nuevoNombreConNumero);
				}

				// 5. Construir respuesta por archivo
				Map<String, String> resultado = new HashMap<>();
				resultado.put("archivo", nombre);
				resultado.put("fecha_usada", fechaResultado);
				resultado.put("fecha_cliente", fechaCliente);
				respuesta.add(resultado);
			} catch (Exception e) {
				e.printStackTrace();
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
			}
		}

		System.out.println();
	    
	    return null;
	}
	
	private String normalizarFecha(String fecha) {
	    // Si ya está en el formato deseado, no hacer nada
	    if (fecha.matches("\\d{4}-\\d{2}-\\d{2}_\\d{2}\\.\\d{2}\\.\\d{2}")) {
	        return fecha;
	    }

	    // Convertir formato EXIF: yyyy:MM:dd HH:mm:ss
	    if (fecha.matches("\\d{4}:\\d{2}:\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
	        try {
	            SimpleDateFormat entrada = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
	            SimpleDateFormat salida = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
	            Date date = entrada.parse(fecha);
	            return salida.format(date);
	        } catch (ParseException e) {
	            return "Fecha EXIF inválida";
	        }
	    }

	    // Convertir ISO 8601: 2023-11-14T12:01:11.000Z
	    if (fecha.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*")) {
	        try {
	            Instant instant = Instant.parse(fecha);
	            LocalDateTime ldt = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
	            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");
	            return formatter.format(ldt);
	        } catch (DateTimeParseException e) {
	            return "Fecha ISO inválida";
	        }
	    }

	    // Formato desconocido
	    return "Formato desconocido: " + fecha;
	}
	
	public boolean validaDirectorio() {
		File directorio = new File(UPLOAD_DIR);
		boolean existe = directorio.exists();
		
		if(!existe) {
			existe = directorio.mkdirs();
		}	
		
		return existe;
	}
	
	// Extrae la extensión de un archivo (.jpg, .png, etc.)
    private static String obtenerExtension(String nombreArchivo) {
        int i = nombreArchivo.lastIndexOf('.');
        return (i > 0) ? nombreArchivo.substring(i).toLowerCase() : "";
    }
	
//	private String convertirIsoAFormatoPersonalizado(String isoDateString) {
//	    try {
//	        // Parsear la cadena ISO como fecha con zona UTC
//	        Instant instant = Instant.parse(isoDateString);
//	        // Convertir a zona local (o usar ZoneOffset.UTC si lo prefieres)
//	        LocalDateTime localDateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
//	        // Formato deseado
//	        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");
//	        return formatter.format(localDateTime);
//	    } catch (DateTimeParseException e) {
//	        return isoDateString;
//	    }
//	}
	
	public void configuraLogger() {
		try {
	        FileHandler fileHandler = new FileHandler("B:\\ShareFolder\\Imagenes\\LOGS\\carga_imagenes.log", true);
	        fileHandler.setFormatter(new SimpleFormatter());
	        logger.addHandler(fileHandler);
	        logger.setLevel(Level.ALL);
	    } catch (Exception e) {
	        logger.severe("[ERROR] - No se pudo configurar el archivo de log: " + e.getMessage());
	    }
	}
}
