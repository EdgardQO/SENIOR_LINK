package com.seniorlink.api.controller;

import com.seniorlink.api.dto.TotalUsuariosResponseDTO;
import com.seniorlink.api.dto.UsuarioDTO;
import com.seniorlink.api.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // Endpoint que devuelve la lista de usuarios con el total contado
    @GetMapping("/total")
    public ResponseEntity<TotalUsuariosResponseDTO> obtenerUsuariosConTotal() {
        return ResponseEntity.ok(usuarioService.obtenerUsuariosConTotal());
    }

    // Endpoint directo para listar únicamente el arreglo de usuarios
    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> listarUsuarios() {
        return ResponseEntity.ok(usuarioService.listarTodosLosUsuarios());
    }
}