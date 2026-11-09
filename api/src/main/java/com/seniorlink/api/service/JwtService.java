package com.seniorlink.api.service;
import com.seniorlink.api.model.entity.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.Locale;
@Service
public class JwtService {
    private final JwtEncoder encoder;
    private final Duration duracion;
    public JwtService(JwtEncoder encoder, @Value("${seniorlink.jwt.expiration-minutes:60}") long minutos) {
        if (minutos < 1 || minutos > 1440) throw new IllegalArgumentException("Duración JWT inválida");
        this.encoder = encoder;
        this.duracion = Duration.ofMinutes(minutos);
    }
    public String generarToken(Usuario usuario) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("senior-link-api")
            .issuedAt(ahora).expiresAt(ahora.plus(duracion)).subject(usuario.getDni())
            .claim("rol", normalizarRol(usuario.getRol().getNombre())).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
    public long expiracionSegundos() { return duracion.toSeconds(); }
    public static String normalizarRol(String rol) {
        return rol == null ? "" : rol.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    }
    public static String rutaPorRol(String rol) {
        return switch (normalizarRol(rol)) {
            case "ADMINISTRADOR" -> "/admin";
            case "SOPORTE" -> "/soporte";
            case "ACOMPANANTE" -> "/acompanante";
            case "ADULTO_MAYOR" -> "/adulto-mayor";
            case "FAMILIAR" -> "/familiar";
            default -> throw new IllegalStateException("Rol de usuario desconocido");
        };
    }
}
