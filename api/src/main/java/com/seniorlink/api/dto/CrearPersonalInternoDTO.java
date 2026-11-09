package com.seniorlink.api.dto;

/**
 * Alta de personal interno. Solo ADMINISTRADOR puede usar este flujo.
 * Los perfiles públicos se registran por /api/v1/auth/registro.
 */
public record CrearPersonalInternoDTO(
        String dni,
        String nombres,
        String apellidos,
        String correoElectronico,
        String contrasena,
        String telefono,
        String rol,
        String areaODepartamento,
        Short nivelAcceso,
        String turno,
        String extensionTelefonica
) {}
