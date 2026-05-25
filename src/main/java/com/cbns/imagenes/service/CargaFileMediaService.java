package com.cbns.imagenes.service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface CargaFileMediaService {
	public ResponseEntity<String> procesarCargaImagen(MultipartFile file, String modifiedDate, String owner);
}
