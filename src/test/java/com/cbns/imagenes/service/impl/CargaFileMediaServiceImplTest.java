package com.cbns.imagenes.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class CargaFileMediaServiceImplTest {
    private CargaFileMediaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CargaFileMediaServiceImpl();
    }

    @Test
    void testObtenerIpLocal_NotNull() {
        String ip = service.obtenerIpLocal();
        assertNotNull(ip);
        assertFalse(ip.isEmpty());
    }

    @Test
    void testObtenerExtension_Jpg() {
        String ext = service.obtenerExtension("foto.jpg");
        assertEquals(".jpg", ext);
    }

    @Test
    void testObtenerExtension_SinExtension() {
        String ext = service.obtenerExtension("archivo");
        assertEquals("", ext);
    }

    @Test
    void testObtenerAnio_FechaNormalizada() {
        assertEquals("2026", service.obtenerAnio("2026-05-24_12.00.00"));
    }

    @Test
    void testObtenerAnio_FechaInvalida() {
        assertThrows(IllegalArgumentException.class, () -> service.obtenerAnio("Fecha inválida"));
    }

    @Test
    void testObtenerDirectorioAnio_UsaUsuarioPhotosYAnio() throws Exception {
        assertEquals(
                Paths.get("/volume1/homes").toAbsolutePath().normalize()
                        .resolve("christian_neri").resolve("Photos").resolve("2026"),
                service.obtenerDirectorioAnio("christian_neri", "2026"));
    }

    @Test
    void testObtenerDirectorioAnio_RechazaRutaFueraDelVolumen() {
        assertThrows(IOException.class, () -> service.obtenerDirectorioAnio("..", "2026"));
    }

    @Test
    void testProcesarCargaImagen_ArchivoVacio() {
        MultipartFile file = Mockito.mock(MultipartFile.class);
        Mockito.when(file.isEmpty()).thenReturn(true);
        ResponseEntity<String> resp = service.procesarCargaImagen(file, "2024-05-24T12:00:00.000Z", "owner");
        assertEquals(400, resp.getStatusCodeValue());
        assertTrue(resp.getBody().contains("No se recibió ningún archivo"));
    }

    @Test
    void testProcesarCargaImagen_UsuarioInvalido() {
        MultipartFile file = Mockito.mock(MultipartFile.class);
        ResponseEntity<String> resp = service.procesarCargaImagen(file, "2024-05-24T12:00:00.000Z", "../owner");
        assertEquals(400, resp.getStatusCodeValue());
        assertEquals("El usuario no es válido.", resp.getBody());
    }

    // NOTA: Para pruebas completas de procesarCargaImagen se recomienda usar un entorno con archivos temporales y mocks avanzados.
}
