package com.seniorlink.api.dto;

import com.seniorlink.api.model.entity.Usuario;

public record LoginResponseDTO(
        String mensaje,
        String dni,
        String nombres,
        String apellidos,
        String correoElectronico,
        Short rolId,
        String rolNombre,
        Boolean estaActivo
) {
    public static LoginResponseDTO desdeUsuario(Usuario u, String mensaje) {
        return new LoginResponseDTO(
                mensaje,
                u.getDni(),
                u.getNombres(),
                u.getApellidos(),
                u.getCorreoElectronico(),
                u.getRol() != null ? u.getRol().getId() : null,
                u.getRol() != null ? u.getRol().getNombre() : null,
                u.getEstaActivo()
        );
    }
}