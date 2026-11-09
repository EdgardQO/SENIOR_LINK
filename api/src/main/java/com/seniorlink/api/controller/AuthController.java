package com.seniorlink.api.controller;

import com.seniorlink.api.dto.LoginRequestDTO;
import com.seniorlink.api.dto.LoginResponseDTO;
import com.seniorlink.api.dto.RegistroUsuarioDTO;
import com.seniorlink.api.dto.UsuarioDTO;
import com.seniorlink.api.service.AuthService;
import com.seniorlink.api.service.RegistroUsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*") // Permite llamadas desde el frontend de Astro/React
public class AuthController {

    private final AuthService authService;
    private final RegistroUsuarioService registroUsuarioService;

    public AuthController(AuthService authService, RegistroUsuarioService registroUsuarioService) {
        this.authService = authService;
        this.registroUsuarioService = registroUsuarioService;
    }

    @PostMapping("/registro")
    public ResponseEntity<UsuarioDTO> registrar(@RequestBody RegistroUsuarioDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registroUsuarioService.registrar(request));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO request) {
        try {
            LoginResponseDTO response = authService.autenticar(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }
}