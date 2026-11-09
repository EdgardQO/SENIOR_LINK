package com.seniorlink.api.controller;

import com.seniorlink.api.dto.TotalUsuariosResponseDTO;
import com.seniorlink.api.dto.UsuarioDTO;
import com.seniorlink.api.dto.UsuarioDetalleDTO;
import com.seniorlink.api.dto.ActualizarUsuarioDTO;
import com.seniorlink.api.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
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
    @GetMapping("/{dni}")
    public ResponseEntity<UsuarioDetalleDTO> obtenerDetalle(@PathVariable String dni) {
        return ResponseEntity.ok(usuarioService.obtenerDetalle(dni));
    }

    /**
     * Actualiza los campos enviados. Los campos omitidos o null se mantienen sin cambios.
     * El DNI es inmutable y la contraseña se gestiona por un flujo separado.
     */
    @PutMapping("/{dni}")
    public ResponseEntity<UsuarioDetalleDTO> actualizar(
            @PathVariable String dni,
            @RequestBody ActualizarUsuarioDTO solicitud) {
        return ResponseEntity.ok(usuarioService.actualizar(dni, solicitud));
    }
}