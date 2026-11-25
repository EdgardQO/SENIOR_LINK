package com.seniorlink.api;

import com.seniorlink.api.controller.UsuarioController;
import com.seniorlink.api.dto.RegistroUsuarioDTO;
import com.seniorlink.api.repository.UsuarioRepository;
import com.seniorlink.api.service.FotoPerfilService;
import com.seniorlink.api.service.RegistroUsuarioService;
import com.seniorlink.api.service.ReservaLocalService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class FotoPerfilIntegrationTests {
    private static final Path DIRECTORIO = Path.of(System.getProperty("java.io.tmpdir"),
            "seniorlink-photo-integration-" + UUID.randomUUID());

    @DynamicPropertySource
    static void photoDirectory(DynamicPropertyRegistry registry) {
        registry.add("seniorlink.uploads.directory", () -> DIRECTORIO.toString());
    }

    @Autowired RegistroUsuarioService registro;
    @Autowired FotoPerfilService fotos;
    @Autowired UsuarioRepository usuarios;
    @Autowired UsuarioController controller;
    @Autowired ReservaLocalService reservas;

    static MockMultipartFile imagen(String formato) throws IOException {
        BufferedImage img = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(img, formato, salida));
        return new MockMultipartFile("foto", "perfil." + formato,
                formato.equals("png") ? "image/png" : "image/jpeg", salida.toByteArray());
    }

    static RegistroUsuarioDTO adulto() {
        return new RegistroUsuarioDTO("96000001", "Rosa", "Prueba", "rosa.photo@seniorlink.test",
                "DemoPhotoPassword2026!", "900000000", "ADULTO_MAYOR",
                new RegistroUsuarioDTO.AdultoMayor(LocalDate.of(1950, 1, 1), "Lima", "Contacto", "999999999", null),
                null, null);
    }

    static RegistroUsuarioDTO acompanante() {
        return new RegistroUsuarioDTO("96000002", "Lucía", "Torres", "lucia.photo@seniorlink.test",
                "DemoPhotoPassword2026!", "900000001", "ACOMPANANTE", null, null,
                new RegistroUsuarioDTO.Acompanante("Acompañamiento cercano", 6,
                        "Enfermería", new BigDecimal("25.00"), new BigDecimal("25.00"), null));
    }

    @Test
    void registroFotoConsultaAutorizadaYReemplazo() throws IOException {
        byte[] original = imagen("png").getBytes();
        var resultado = registro.registrarConFoto(adulto(), imagen("png"));
        assertEquals("/api/v1/usuarios/96000001/foto", resultado.fotoUrl());
        String primeraRuta = usuarios.findById("96000001").orElseThrow().getFotoPerfilRuta();
        assertTrue(primeraRuta.startsWith("96000001/perfil-"));
        assertTrue(primeraRuta.endsWith(".png"));
        assertArrayEquals(original, fotos.obtener("96000001").bytes());

        var propio = new UsernamePasswordAuthenticationToken("96000001", "unused", List.of());
        var externo = new UsernamePasswordAuthenticationToken("96000003", "unused", List.of());
        assertEquals(200, controller.obtenerFoto(propio, "96000001").getStatusCode().value());
        assertThrows(ResponseStatusException.class, () -> controller.obtenerFoto(externo, "96000001"));

        fotos.guardar("96000001", imagen("jpeg"));
        String nuevaRuta = usuarios.findById("96000001").orElseThrow().getFotoPerfilRuta();
        assertTrue(nuevaRuta.startsWith("96000001/perfil-"));
        assertTrue(nuevaRuta.endsWith(".jpg"));
        assertNotEquals(primeraRuta, nuevaRuta);
        assertEquals("image/jpeg", fotos.obtener("96000001").tipo());
    }

    @Test
    void rechazoArchivoInvalidoAntesDeRegistrar() {
        var invalida = new MockMultipartFile("foto", "texto.png", "image/png",
                "esto no es una imagen".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThrows(ResponseStatusException.class, () -> registro.registrarConFoto(adulto(), invalida));
        assertFalse(usuarios.existsById("96000001"));
    }

    @Test
    void catalogoAcompanantesIncluyeFotoDatosYAccesoDeLectura() throws IOException {
        registro.registrarConFoto(acompanante(), imagen("png"));
        var perfil = reservas.acompanantes().stream()
                .filter(item -> item.dni().equals("96000002"))
                .findFirst().orElseThrow();
        assertEquals("Enfermería", perfil.carrera());
        assertEquals(6, perfil.ciclo());
        assertEquals("Acompañamiento cercano", perfil.biografia());
        assertEquals(new BigDecimal("25.00"), perfil.tarifaHora());
        assertEquals("/api/v1/usuarios/96000002/foto", perfil.fotoUrl());

        var otroUsuario = new UsernamePasswordAuthenticationToken("96000003", "unused", List.of());
        assertEquals(200, controller.obtenerFoto(otroUsuario, "96000002").getStatusCode().value());
    }

    @AfterAll
    static void limpiar() throws IOException {
        if (!Files.exists(DIRECTORIO)) return;
        try (var rutas = Files.walk(DIRECTORIO)) {
            for (Path ruta : rutas.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(ruta);
            }
        }
    }
}
