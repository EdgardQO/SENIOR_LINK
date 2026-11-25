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

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UsuarioDTO> crear(@RequestPart("datos") CrearPersonalInternoDTO solicitud, @RequestPart("foto") org.springframework.web.multipart.MultipartFile foto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crearConFoto(solicitud, foto));
    }
}
