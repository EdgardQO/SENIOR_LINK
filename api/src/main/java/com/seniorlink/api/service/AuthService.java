package com.seniorlink.api.service;
import com.seniorlink.api.dto.*;
import com.seniorlink.api.model.entity.Usuario;
import com.seniorlink.api.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;
@Service
public class AuthService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }
    @Transactional(readOnly = true)
    public LoginResponseDTO autenticar(LoginRequestDTO request) {
        if (request == null || request.correoElectronico() == null || request.correoElectronico().isBlank()
            || request.contrasena() == null || request.contrasena().isBlank())
            throw new IllegalArgumentException("Correo electrónico y contraseña son obligatorios");
        Usuario usuario = usuarioRepository.findByCorreoElectronico(request.correoElectronico().trim().toLowerCase(Locale.ROOT))
            .orElseThrow(() -> new IllegalArgumentException("Correo o contraseña incorrectos"));
        if (!Boolean.TRUE.equals(usuario.getEstaActivo())) throw new IllegalStateException("Cuenta inactiva");
        if (!passwordEncoder.matches(request.contrasena(), usuario.getContrasenaHash()))
            throw new IllegalArgumentException("Correo o contraseña incorrectos");
        if (usuario.getRol() == null) throw new IllegalStateException("La cuenta no tiene rol asignado");
        String token = jwtService.generarToken(usuario);
        return LoginResponseDTO.desdeUsuario(usuario, "Inicio de sesión exitoso", token, jwtService.expiracionSegundos());
    }
}
