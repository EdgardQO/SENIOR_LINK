package com.seniorlink.api.controller;

import com.seniorlink.api.dto.LoginRequestDTO;
import com.seniorlink.api.dto.LoginResponseDTO;
import com.seniorlink.api.dto.RegistroUsuarioDTO;
import com.seniorlink.api.dto.UsuarioDTO;
import com.seniorlink.api.service.AuthService;
import com.seniorlink.api.service.RegistroUsuarioService;
import com.seniorlink.api.service.FotoPerfilService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final RegistroUsuarioService registroUsuarioService;
    private final FotoPerfilService fotos;
    private final ObjectMapper objectMapper;

    public AuthController(AuthService authService, RegistroUsuarioService registroUsuarioService, FotoPerfilService fotos, ObjectMapper objectMapper) {
        this.authService = authService;
        this.registroUsuarioService = registroUsuarioService;
        this.fotos = fotos;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/registro", consumes = "multipart/form-data")
    public ResponseEntity<UsuarioDTO> registrarConFoto(
            @RequestPart("datos") String datos,
            @RequestPart("foto") MultipartFile foto) throws com.fasterxml.jackson.core.JsonProcessingException {
        fotos.validar(foto);
        RegistroUsuarioDTO solicitud = objectMapper.readValue(datos, RegistroUsuarioDTO.class);
        return ResponseEntity.status(HttpStatus.CREATED).body(registroUsuarioService.registrarConFoto(solicitud, foto));
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