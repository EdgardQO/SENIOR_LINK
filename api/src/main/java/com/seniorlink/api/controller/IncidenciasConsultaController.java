package com.seniorlink.api.controller;

import com.seniorlink.api.service.PostServicioLocalService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** Consultas y resoluciones de incidencias, sin habilitar la pasarela local. */
@RestController
@RequestMapping("/api/v1/incidencias")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','SOPORTE')")
public class IncidenciasConsultaController {
    private final PostServicioLocalService servicio;

    public IncidenciasConsultaController(PostServicioLocalService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<PostServicioLocalService.IncidenciaVista> listar(Authentication auth) {
        return servicio.incidencias(auth.getName());
    }

    public record Resolucion(String resolucion) {}

    @PostMapping("/{id}/resolver")
    public PostServicioLocalService.IncidenciaVista resolver(
            Authentication auth, @PathVariable Long id, @RequestBody Resolucion input) {
        return servicio.resolver(auth.getName(), id, input == null ? null : input.resolucion());
    }
}
