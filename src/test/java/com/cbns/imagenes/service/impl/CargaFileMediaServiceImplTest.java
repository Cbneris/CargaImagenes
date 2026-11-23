package com.cbns.imagenes.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.cbns.imagenes.util.FechaUtil;

import static org.junit.jupiter.api.Assertions.*;

class CargaFileMediaServiceImplTest {
    private CargaFileMediaServiceImpl service;

    @TempDir
    Path homes;

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
    void testObtenerOwnersDisponibles_ListaSoloDirectoriosValidos() throws IOException {
        Files.createDirectory(homes.resolve("alice"));
        Files.createDirectory(homes.resolve("bob-2"));
        Files.createFile(homes.resolve("archivo.txt"));
        Files.createDirectory(homes.resolve("invalid owner"));
        ReflectionTestUtils.setField(service, "uploadDir", homes.toString());

        assertEquals(List.of("alice", "bob-2"), service.obtenerOwnersDisponibles());
    }

    @Test
    void testProcesarCargaImagen_VideoUsaFechaDeModificacion() throws IOException {
        ReflectionTestUtils.setField(service, "uploadDir", homes.toString());
        MultipartFile file = Mockito.mock(MultipartFile.class);
        Mockito.when(file.isEmpty()).thenReturn(false);
        Mockito.when(file.getContentType()).thenReturn("video/mp4");
        Mockito.when(file.getOriginalFilename()).thenReturn("video.mp4");
        Mockito.when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[] {1, 2, 3}));
        Mockito.doAnswer(invocation -> {
            File destination = invocation.getArgument(0);
            Files.write(destination.toPath(), new byte[] {1, 2, 3});
            return null;
        }).when(file).transferTo(Mockito.any(File.class));

        String modifiedDate = "2024-05-24T12:00:00Z";
        String expectedDate = FechaUtil.normalizarFecha(modifiedDate);
        ResponseEntity<String> response = service.procesarCargaImagen(file, modifiedDate, "alice");

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(Files.exists(homes.resolve("alice/Photos")
                .resolve(expectedDate.substring(0, 4)).resolve(expectedDate + ".mp4")));
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
