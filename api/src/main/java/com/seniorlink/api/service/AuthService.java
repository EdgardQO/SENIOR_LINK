package com.seniorlink.api.service;

import com.seniorlink.api.dto.LoginRequestDTO;
import com.seniorlink.api.dto.LoginResponseDTO;
import com.seniorlink.api.model.entity.Usuario;
import com.seniorlink.api.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public LoginResponseDTO autenticar(LoginRequestDTO request) {
        // 1. Buscar usuario por correo electrónico
        Usuario usuario = usuarioRepository.findByCorreoElectronico(request.correoElectronico().trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Credenciales incorrectas: correo o contraseña inválidos."));

        // 2. Comprobar si la cuenta está habilitada
        if (!Boolean.TRUE.equals(usuario.getEstaActivo())) {
            throw new IllegalStateException("Tu cuenta se encuentra inactiva. Comunícate con soporte.");
        }

        // 3. Comparar la contraseña ingresada con el hash guardado en BD
        boolean passwordValida = passwordEncoder.matches(request.contrasena(), usuario.getContrasenaHash());

        // Soporte de compatibilidad si alguna clave fue guardada en texto plano durante pruebas:
        if (!passwordValida && request.contrasena().equals(usuario.getContrasenaHash())) {
            passwordValida = true;
        }

        if (!passwordValida) {
            throw new IllegalArgumentException("Credenciales incorrectas: correo o contraseña inválidos.");
        }

        // 4. Retornar respuesta con el rol
        return LoginResponseDTO.desdeUsuario(usuario, "Inicio de sesión exitoso");
    }
}