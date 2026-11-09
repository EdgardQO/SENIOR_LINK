package com.seniorlink.api.service;

import com.seniorlink.api.dto.CrearPersonalInternoDTO;
import com.seniorlink.api.dto.UsuarioDTO;
import com.seniorlink.api.model.entity.*;
import com.seniorlink.api.repository.*;
import com.seniorlink.api.util.RolesConstantes;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class PersonalInternoService {
    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final PerfilAdminRepository administradores;
    private final PerfilSoporteRepository soportes;
    private final PasswordEncoder encoder;

    public PersonalInternoService(UsuarioRepository usuarios, RolRepository roles,
                                  PerfilAdminRepository administradores, PerfilSoporteRepository soportes,
                                  PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.administradores = administradores;
        this.soportes = soportes;
        this.encoder = encoder;
    }

    @Transactional
    public UsuarioDTO crear(CrearPersonalInternoDTO dto) {
        if (dto == null) throw badRequest("Debes enviar los datos del personal");
        String rolNombre = requerido(dto.rol(), "rol").toUpperCase(Locale.ROOT).replace(' ', '_');
        if (!rolNombre.equals("ADMINISTRADOR") && !rolNombre.equals("SOPORTE"))
            throw badRequest("Solo se permite crear personal ADMINISTRADOR o SOPORTE en este endpoint");

        String dni = requerido(dto.dni(), "DNI");
        if (!dni.matches("\\d{8}")) throw badRequest("El DNI debe tener 8 dígitos");
        String nombres = requerido(dto.nombres(), "nombres");
        String apellidos = requerido(dto.apellidos(), "apellidos");
        if (nombres.length() > 100 || apellidos.length() > 100)
            throw badRequest("Nombres y apellidos admiten hasta 100 caracteres");

        String correo = requerido(dto.correoElectronico(), "correo electrónico").toLowerCase(Locale.ROOT);
        if (correo.length() > 150 || !correo.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
            throw badRequest("Correo electrónico inválido");
        String telefono = opcional(dto.telefono());
        if (telefono != null && telefono.length() > 20) throw badRequest("Teléfono demasiado largo");
        if (dto.contrasena() == null || dto.contrasena().length() < 8 || dto.contrasena().length() > 72)
            throw badRequest("La contraseña debe tener entre 8 y 72 caracteres");

        if (usuarios.existsById(dni)) throw conflict("El DNI ya está registrado");
        if (usuarios.existsByCorreoElectronico(correo)) throw conflict("El correo electrónico ya está registrado");

        Short rolId = rolNombre.equals("ADMINISTRADOR") ? RolesConstantes.ADMIN : RolesConstantes.SOPORTE;
        Rol rol = roles.findById(rolId)
                .orElseThrow(() -> badRequest("No existe el rol " + rolNombre + " en la tabla roles"));
        if (!rolNombre.equals(JwtService.normalizarRol(rol.getNombre())))
            throw badRequest("El ID y nombre del rol no coinciden en la tabla roles");

        String area = null, turno = null, extension = null;
        Short nivel = null;
        if (rolNombre.equals("ADMINISTRADOR")) {
            area = requerido(dto.areaODepartamento(), "área o departamento");
            if (area.length() > 100) throw badRequest("El área admite hasta 100 caracteres");
            nivel = dto.nivelAcceso() == null ? (short) 1 : dto.nivelAcceso();
            if (nivel < 1) throw badRequest("El nivel de acceso debe ser mayor que cero");
            if (dto.turno() != null || dto.extensionTelefonica() != null)
                throw badRequest("Los campos de soporte no corresponden al perfil administrador");
        } else {
            turno = requerido(dto.turno(), "turno");
            if (turno.length() > 50) throw badRequest("El turno admite hasta 50 caracteres");
            extension = opcional(dto.extensionTelefonica());
            if (extension != null && extension.length() > 20)
                throw badRequest("La extensión admite hasta 20 caracteres");
            if (dto.areaODepartamento() != null || dto.nivelAcceso() != null)
                throw badRequest("Los campos de administrador no corresponden al perfil soporte");
        }

        Usuario usuario = new Usuario();
        usuario.setDni(dni);
        usuario.setNombres(nombres);
        usuario.setApellidos(apellidos);
        usuario.setCorreoElectronico(correo);
        usuario.setContrasenaHash(encoder.encode(dto.contrasena()));
        usuario.setTelefono(telefono);
        usuario.setRol(rol);
        usuario.setEstaActivo(true);
        usuarios.save(usuario);

        if (rolNombre.equals("ADMINISTRADOR")) {
            administradores.save(new PerfilAdmin(usuario, area, nivel));
        } else {
            soportes.save(new PerfilSoporte(usuario, turno, extension));
        }
        return UsuarioDTO.desdeEntidad(usuario);
    }

    private static String requerido(String value, String campo) {
        if (value == null || value.isBlank()) throw badRequest("El campo " + campo + " es obligatorio");
        return value.trim();
    }
    private static String opcional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
    private static ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
