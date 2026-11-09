package com.seniorlink.api.dto;

import com.seniorlink.api.model.entity.Usuario;
import java.time.OffsetDateTime;

public record UsuarioDTO(
        String dni,
        Short rolId,
        String rolNombre,
        String correoElectronico,
        String nombres,
        String apellidos,
        String telefono,
        Boolean estaActivo,
        OffsetDateTime creadoEn,
        OffsetDateTime actualizadoEn
) {
    public static UsuarioDTO desdeEntidad(Usuario u) {
        return new UsuarioDTO(
                u.getDni(),
                u.getRol() != null ? u.getRol().getId() : null,
                u.getRol() != null ? u.getRol().getNombre() : null,
                u.getCorreoElectronico(),
                u.getNombres(),
                u.getApellidos(),
                u.getTelefono(),
                u.getEstaActivo(),
                u.getCreadoEn(),
                u.getActualizadoEn()
        );
    }
}