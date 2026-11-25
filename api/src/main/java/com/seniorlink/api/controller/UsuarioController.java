package com.seniorlink.api.controller;

import com.seniorlink.api.dto.TotalUsuariosResponseDTO;
import com.seniorlink.api.dto.UsuarioDTO;
import com.seniorlink.api.dto.UsuarioDetalleDTO;
import com.seniorlink.api.dto.ActualizarUsuarioDTO;
import com.seniorlink.api.service.UsuarioService;
import com.seniorlink.api.service.FotoPerfilService;
import com.seniorlink.api.repository.UsuarioRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final FotoPerfilService fotos;
    private final UsuarioRepository usuarios;

    public UsuarioController(UsuarioService usuarioService, FotoPerfilService fotos, UsuarioRepository usuarios) {
        this.usuarioService = usuarioService;
        this.fotos = fotos;
        this.usuarios = usuarios;
    }

    public record ContactoPropio(String telefono, String telefonoAlternativo) {}
    @PreAuthorize("hasAnyRole('ACOMPANANTE','FAMILIAR','ADULTO_MAYOR')")
    @PutMapping("/me/contacto")
    public ResponseEntity<UsuarioDetalleDTO> actualizarContacto(Authentication auth, @RequestBody ContactoPropio input) {
        return ResponseEntity.ok(usuarioService.actualizarContactoPropio(auth.getName(), input.telefono(), input.telefonoAlternativo()));
    }

    @PutMapping(value = "/me/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UsuarioDTO> actualizarFoto(Authentication auth, @RequestPart("foto") MultipartFile foto) {
        fotos.guardar(auth.getName(), foto);
        return ResponseEntity.ok(UsuarioDTO.desdeEntidad(usuarios.findById(auth.getName()).orElseThrow()));
    }

    @GetMapping("/{dni}/foto")
    public ResponseEntity<byte[]> obtenerFoto(Authentication auth, @PathVariable String dni) {
        var usuario = usuarios.findById(dni).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        boolean propia = auth.getName().equals(dni);
        boolean admin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"));
        boolean acompanante = usuario.getRol() != null &&
            "ACOMPANANTE".equals(usuario.getRol().getNombre()) && Boolean.TRUE.equals(usuario.getEstaActivo());
        if (!propia && !admin && !acompanante) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        FotoPerfilService.Foto foto = fotos.obtener(dni);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(foto.tipo()))
            .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
            .header("X-Content-Type-Options", "nosniff")
            .body(foto.bytes());
    }

    @GetMapping("/total")
    public ResponseEntity<TotalUsuariosResponseDTO> obtenerUsuariosConTotal() {
        return ResponseEntity.ok(usuarioService.obtenerUsuariosConTotal());
    }

    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> listarUsuarios() {
        return ResponseEntity.ok(usuarioService.listarTodosLosUsuarios());
    }

    /**
     * Devuelve los datos generales y el perfil específico del usuario.
     */
    @PreAuthorize("hasRole('ADMINISTRADOR') or #dni == authentication.name")
    @GetMapping("/{dni}")
    public ResponseEntity<UsuarioDetalleDTO> obtenerDetalle(@PathVariable String dni) {
        return ResponseEntity.ok(usuarioService.obtenerDetalle(dni));
    }

    /**
     * Actualiza los campos enviados. Los campos omitidos o null se mantienen sin cambios.
     * El DNI es inmutable y la contraseña se gestiona por un flujo separado.
     */
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PutMapping("/{dni}")
    public ResponseEntity<UsuarioDetalleDTO> actualizar(
            @PathVariable String dni,
            @RequestBody ActualizarUsuarioDTO solicitud) {
        return ResponseEntity.ok(usuarioService.actualizar(dni, solicitud));
    }
}