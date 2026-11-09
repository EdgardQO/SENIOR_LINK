package com.seniorlink.api.controller;

import com.seniorlink.api.dto.CrearPersonalInternoDTO;
import com.seniorlink.api.dto.UsuarioDTO;
import com.seniorlink.api.service.PersonalInternoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/personal")
public class PersonalInternoController {
    private final PersonalInternoService servicio;

    public PersonalInternoController(PersonalInternoService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UsuarioDTO> crear(@RequestBody CrearPersonalInternoDTO solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(solicitud));
    }
}
