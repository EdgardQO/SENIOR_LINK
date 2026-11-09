package com.seniorlink.api.controller;

import com.seniorlink.api.dto.InstitucionAliadaDTO;
import com.seniorlink.api.service.AcompananteInstitucionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class AcompananteInstitucionController {

    private final AcompananteInstitucionService service;

    public AcompananteInstitucionController(AcompananteInstitucionService service) {
        this.service = service;
    }

    @GetMapping("/instituciones-aliadas")
    public ResponseEntity<List<InstitucionAliadaDTO>> listarInstituciones() {
        return ResponseEntity.ok(service.listarInstituciones());
    }

    @GetMapping("/acompanantes/{dni}/institucion")
    public ResponseEntity<InstitucionAliadaDTO> obtenerInstitucion(@PathVariable String dni) {
        return ResponseEntity.ok(service.obtenerPorAcompanante(dni));
    }

    @PutMapping("/acompanantes/{dni}/institucion/{ruc}")
    public ResponseEntity<Void> asignarInstitucion(@PathVariable String dni, @PathVariable String ruc) {
        service.asignarInstitucion(dni, ruc);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/acompanantes/{dni}/institucion")
    public ResponseEntity<Void> quitarInstitucion(@PathVariable String dni) {
        service.quitarInstitucion(dni);
        return ResponseEntity.noContent().build();
    }
}
