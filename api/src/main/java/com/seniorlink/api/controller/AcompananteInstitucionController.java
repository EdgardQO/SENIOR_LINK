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

    @GetMapping("/acompanantes/{dni}/instituciones")
    public ResponseEntity<List<InstitucionAliadaDTO>> listarPorAcompanante(@PathVariable String dni) {
        return ResponseEntity.ok(service.listarPorAcompanante(dni));
    }

    @PostMapping("/acompanantes/{dni}/instituciones/{ruc}")
    public ResponseEntity<Void> vincular(@PathVariable String dni, @PathVariable String ruc) {
        service.vincular(dni, ruc);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/acompanantes/{dni}/instituciones/{ruc}")
    public ResponseEntity<Void> desvincular(@PathVariable String dni, @PathVariable String ruc) {
        service.desvincular(dni, ruc);
        return ResponseEntity.noContent().build();
    }
}
