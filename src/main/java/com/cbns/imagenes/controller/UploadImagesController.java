package com.cbns.imagenes.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cbns.imagenes.constantes.ConstantesTecnicas;
import com.cbns.imagenes.service.CargaFileMediaService;

@RestController
@RequestMapping(ConstantesTecnicas.RUTA_CONTROLADOR_CARGA)
@CrossOrigin(origins = ConstantesTecnicas.ORIGEN_CORS_FRONTEND)
public class UploadImagesController {

	private final CargaFileMediaService cargaFileMediaService;

	public UploadImagesController(CargaFileMediaService cargaFileMediaService) {
		this.cargaFileMediaService = cargaFileMediaService;
	}

	@GetMapping(ConstantesTecnicas.RUTA_OWNERS)
	public ResponseEntity<List<String>> obtenerOwners() throws IOException {
		return ResponseEntity.ok(cargaFileMediaService.obtenerOwnersDisponibles());
	}
	
	@PostMapping(ConstantesTecnicas.RUTA_UPLOAD)
	public ResponseEntity<?> cargaImagenes(
			@RequestParam(ConstantesTecnicas.PARAMETRO_IMAGES) MultipartFile file,
			@RequestParam(ConstantesTecnicas.PARAMETRO_MODIFIED_DATE) String modifiedDate,
			@RequestParam(ConstantesTecnicas.PARAMETRO_OWNER) String owner) {
		return cargaFileMediaService.procesarCargaImagen(file, modifiedDate, owner);
	}

	@PostMapping(ConstantesTecnicas.RUTA_UPLOAD_COMPLETE)
	public ResponseEntity<String> registrarResumenCarga(
			@RequestParam(ConstantesTecnicas.PARAMETRO_OWNER) String owner,
			@RequestParam(ConstantesTecnicas.PARAMETRO_UPLOADED_COUNT) int cantidadCargada) {
		return cargaFileMediaService.registrarResumenCarga(owner, cantidadCargada);
	}
	
}
