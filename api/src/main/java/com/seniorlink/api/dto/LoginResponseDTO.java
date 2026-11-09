package com.seniorlink.api.dto;
import com.seniorlink.api.model.entity.Usuario;
import com.seniorlink.api.service.JwtService;
public record LoginResponseDTO(
    String mensaje, String dni, String nombres, String apellidos,
    String correoElectronico, Short rolId, String rolNombre, Boolean estaActivo,
    String accessToken, String tokenType, long expiresIn, String rutaInicio
) {
    public static LoginResponseDTO desdeUsuario(Usuario u, String mensaje, String token, long expiracion) {
        return new LoginResponseDTO(mensaje, u.getDni(), u.getNombres(), u.getApellidos(),
            u.getCorreoElectronico(), u.getRol().getId(), u.getRol().getNombre(),
            u.getEstaActivo(), token, "Bearer", expiracion, JwtService.rutaPorRol(u.getRol().getNombre()));
    }
}
