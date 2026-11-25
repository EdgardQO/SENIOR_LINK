package com.seniorlink.api.service;

import com.seniorlink.api.model.entity.Usuario;
import com.seniorlink.api.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class FotoPerfilService {
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Pattern DNI = Pattern.compile("[0-9]{8}");
    private final Path root;
    private final UsuarioRepository usuarios;

    public FotoPerfilService(UsuarioRepository usuarios,
                             @Value("${seniorlink.uploads.directory:./uploads}") String directorio) {
        this.usuarios = usuarios;
        this.root = Paths.get(directorio).toAbsolutePath().normalize().resolve("usuarios");
    }

    public record Foto(String tipo, byte[] bytes) {}

    public void validar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty() || archivo.getSize() > MAX_BYTES)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fotografía es obligatoria y debe pesar hasta 5 MB");
        if (archivo.getOriginalFilename() != null && archivo.getOriginalFilename().length() > 255)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nombre de archivo inválido");
        try {
            byte[] datos = archivo.getBytes();
            if (datos.length == 0 || datos.length > MAX_BYTES)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tamaño de imagen inválido");
            String formato = detectarFormato(datos);
            if (formato == null)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo se permiten imágenes JPG, PNG o WebP válidas");
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo leer la imagen", e);
        }
    }

    private String detectarFormato(byte[] bytes) throws IOException {
        if (bytes.length < 16) return null;
        String tipo = null;
        if ((bytes[0] & 255) == 255 && (bytes[1] & 255) == 216 && (bytes[2] & 255) == 255)
            tipo = "jpg";
        else if ((bytes[0] & 255) == 137 && bytes[1] == 80 && bytes[2] == 78 && bytes[3] == 71 &&
                 bytes[4] == 13 && bytes[5] == 10 && bytes[6] == 26 && bytes[7] == 10)
            tipo = "png";
        else if (bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F' &&
                 bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P')
            tipo = "webp";
        if (tipo == null) return null;
        if (!tipo.equals("webp")) {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (img == null || img.getWidth() < 1 || img.getHeight() < 1 ||
                (long) img.getWidth() * img.getHeight() > 24_000_000L) return null;
        } else {
            // Los proveedores de ImageIO no incluyen WebP por defecto. Se comprueba la cabecera RIFF
            // y el tipo de fragmento estándar, además del límite de tamaño del archivo.
            String chunk = new String(bytes, 12, 4, java.nio.charset.StandardCharsets.US_ASCII);
            if (!Set.of("VP8 ", "VP8L", "VP8X").contains(chunk)) return null;
            long declared = Integer.toUnsignedLong(java.nio.ByteBuffer.wrap(bytes, 4, 4)
                .order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt());
            if (declared + 8 != bytes.length) return null;
        }
        return tipo;
    }

    @Transactional
    public void guardar(String dni, MultipartFile archivo) {
        validar(archivo);
        if (!DNI.matcher(dni).matches()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identificador inválido");
        Usuario usuario = usuarios.findById(dni).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        try {
            byte[] datos = archivo.getBytes();
            String extension = detectarFormato(datos);
            if (extension == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Imagen inválida");
            Path dir = root.resolve(dni).normalize();
            if (!dir.startsWith(root)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ruta no válida");
            Files.createDirectories(dir);
            Path temporal = Files.createTempFile(dir, "nueva-", ".tmp");
            try {
                Files.write(temporal, datos, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
                Path destino = dir.resolve("perfil." + extension);
                try {
                    Files.move(temporal, destino, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException ex) {
                    Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING);
                }
                String anterior = usuario.getFotoPerfilRuta();
                usuario.setFotoPerfilRuta(dni + "/perfil." + extension);
                usuario.setActualizadoEn(OffsetDateTime.now());
                usuarios.save(usuario);
                if (anterior != null && !anterior.equals(usuario.getFotoPerfilRuta())) {
                    Path vieja = root.resolve(anterior).normalize();
                    if (vieja.startsWith(dir)) Files.deleteIfExists(vieja);
                }
            } finally {
                Files.deleteIfExists(temporal);
            }
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar la fotografía", ex);
        }
    }

    @Transactional(readOnly = true)
    public Foto obtener(String dni) {
        if (!DNI.matcher(dni).matches()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        Usuario usuario = usuarios.findById(dni).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String ruta = usuario.getFotoPerfilRuta();
        if (ruta == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Foto no disponible");
        Path dir = root.resolve(dni).normalize();
        Path archivo = root.resolve(ruta).normalize();
        if (!archivo.startsWith(dir)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        try {
            byte[] datos = Files.readAllBytes(archivo);
            if (datos.length > MAX_BYTES) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            String extension = detectarFormato(datos);
            if (extension == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            return new Foto(switch(extension) {
                case "jpg" -> "image/jpeg";
                case "png" -> "image/png";
                default -> "image/webp";
            }, datos);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Foto no disponible");
        }
    }
}
