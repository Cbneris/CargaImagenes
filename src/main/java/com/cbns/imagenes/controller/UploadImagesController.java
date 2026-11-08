package com.cbns.imagenes.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cbns.imagenes.service.CargaFileMediaService;

@RestController
@RequestMapping("/cargaImagenes")
@CrossOrigin(origins = "http://192.168.1.13:5174")
public class UploadImagesController {

	private final CargaFileMediaService cargaFileMediaService;

	public UploadImagesController(CargaFileMediaService cargaFileMediaService) {
		this.cargaFileMediaService = cargaFileMediaService;
	}
	
	@PostMapping("/upload")
    public ResponseEntity<?> cargaImagenes(
    		@RequestParam("images") 		MultipartFile file,
            @RequestParam("modifiedDate") 	String modifiedDate,
			@RequestParam("owner") 			String owner) {
        return cargaFileMediaService.procesarCargaImagen(file, modifiedDate, owner);
    }
	
}
