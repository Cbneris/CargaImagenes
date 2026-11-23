package com.cbns.imagenes.service;

import java.io.IOException;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface CargaFileMediaService {
	ResponseEntity<String> procesarCargaImagen(MultipartFile file, String modifiedDate, String owner);

	ResponseEntity<String> registrarResumenCarga(String owner, int cantidadCargada);

	List<String> obtenerOwnersDisponibles() throws IOException;

	/**
	 * Obtiene la IP local del equipo (no localhost).
	 * @return IP local como String
	 */
	String obtenerIpLocal();
}
