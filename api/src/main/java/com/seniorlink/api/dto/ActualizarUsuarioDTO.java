package com.seniorlink.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO parcial: los campos null u omitidos conservan el valor actual.
 * No admite cambiar DNI, rol ni contraseña.
 */
public record ActualizarUsuarioDTO(
        String correoElectronico,
        String nombres,
        String apellidos,
        String telefono,
        Boolean estaActivo,
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

    public record Familiar(Short parentescoPrincipalId, String telefonoAlternativo) {}

    public record Acompanante(
            String biografia,
            String nivelAcademico,
            BigDecimal tarifaBasePorHora,
            BigDecimal tarifaActualPorHora
    ) {}

    public record Soporte(String turno, String extensionTelefonica) {}

    public record Admin(String areaODepartamento, Short nivelAcceso) {}
}