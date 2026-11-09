package com.seniorlink.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RegistroUsuarioDTO(
        String dni,
        String nombres,
        String apellidos,
        String correoElectronico,
        String contrasena,
        String telefono,
        String rol,
        AdultoMayor adultoMayor,
        Familiar familiar,
        Acompanante acompanante
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
            String telefonoAlternativo
    ) {}

    public record Acompanante(
            String biografia,
            String nivelAcademico,
            BigDecimal tarifaBasePorHora,
            BigDecimal tarifaActualPorHora,
            String institucionRuc
    ) {}
}
