package com.cbns.imagenes.service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface CargaFileMediaService {
	ResponseEntity<String> procesarCargaImagen(MultipartFile file, String modifiedDate, String owner);

	/**
	 * Obtiene la IP local del equipo (no localhost).
	 * @return IP local como String
	 */
	String obtenerIpLocal();
}
