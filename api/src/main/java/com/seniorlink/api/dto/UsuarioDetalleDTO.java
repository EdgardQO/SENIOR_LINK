package com.seniorlink.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UsuarioDetalleDTO(
        UsuarioDTO usuario,
        AdultoMayor adultoMayor,
        Familiar familiar,
        Acompanante acompanante,
        Soporte soporte,
        Admin admin
) {
    public record AdultoMayor(
            LocalDate fechaNacimiento,
            String direccionResidencia,
            String contactoEmergencia,
            String telefonoEmergencia,
            String observacionesMedicas
    ) {}

    public record Familiar(
            Short parentescoPrincipalId,
            String parentescoPrincipalNombre,
            String telefonoAlternativo
    ) {}

    public record Acompanante(
            String biografia,
            String nivelAcademico,
            BigDecimal tarifaBasePorHora,
            BigDecimal tarifaActualPorHora,
            BigDecimal promedioCalificacion,
            Integer totalCalificaciones
    ) {}

    public record Soporte(String turno, String extensionTelefonica, Integer casosAtendidos) {}

    public record Admin(String areaODepartamento, Short nivelAcceso) {}
}