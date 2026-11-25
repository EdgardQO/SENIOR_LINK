package com.seniorlink.api.controller;

import com.seniorlink.api.service.PostServicioLocalService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * Consulta de liquidaciones disponible sin activar las operaciones de pago simuladas.
 * El servicio aplica filtrado por acompañante y acceso total para administración.
 */
@RestController
@RequestMapping("/api/v1/liquidaciones")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ACOMPANANTE')")
public class LiquidacionesConsultaController {
    private final PostServicioLocalService servicio;

    public LiquidacionesConsultaController(PostServicioLocalService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<PostServicioLocalService.LiquidacionVista> listar(Authentication autenticacion) {
        return servicio.liquidaciones(autenticacion.getName());
    }
}
