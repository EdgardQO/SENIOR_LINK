package com.seniorlink.api.service;

import com.seniorlink.api.dto.RegistroUsuarioDTO;
import com.seniorlink.api.dto.UsuarioDTO;
import com.seniorlink.api.model.entity.*;
import com.seniorlink.api.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

@Service
public class RegistroUsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PerfilAdultoMayorRepository adultoMayorRepository;
    private final PerfilFamiliarRepository familiarRepository;
    private final PerfilAcompananteRepository acompananteRepository;
    private final ParentescoRepository parentescoRepository;
    private final InstitucionAliadaRepository institucionRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistroUsuarioService(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PerfilAdultoMayorRepository adultoMayorRepository,
            PerfilFamiliarRepository familiarRepository,
            PerfilAcompananteRepository acompananteRepository,
            ParentescoRepository parentescoRepository,
            InstitucionAliadaRepository institucionRepository,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.adultoMayorRepository = adultoMayorRepository;
        this.familiarRepository = familiarRepository;
        this.acompananteRepository = acompananteRepository;
        this.parentescoRepository = parentescoRepository;
        this.institucionRepository = institucionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UsuarioDTO registrar(RegistroUsuarioDTO dto) {
        if (dto == null) throw badRequest("Debe enviar los datos de registro");
        String dni = requerido(dto.dni(), "DNI");
        if (!dni.matches("\\d{8}")) throw badRequest("El DNI debe contener 8 dígitos");
        String nombres = requerido(dto.nombres(), "nombres");
        String apellidos = requerido(dto.apellidos(), "apellidos");
        String correo = requerido(dto.correoElectronico(), "correo electrónico").toLowerCase(Locale.ROOT);
        if (!correo.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") || correo.length() > 150) {
            throw badRequest("El correo electrónico no tiene un formato válido");
        }
        if (nombres.length() > 100 || apellidos.length() > 100) {
            throw badRequest("Nombres y apellidos deben tener como máximo 100 caracteres");
        }
        String telefono = opcional(dto.telefono());
        if (telefono != null && telefono.length() > 20) throw badRequest("El teléfono es demasiado largo");
        if (dto.contrasena() == null || dto.contrasena().length() < 8 || dto.contrasena().length() > 72) {
            throw badRequest("La contraseña debe contener entre 8 y 72 caracteres");
        }

        String rolSolicitado = requerido(dto.rol(), "rol").toUpperCase(Locale.ROOT)
                .replace(' ', '_');
        if (!rolSolicitado.equals("ADULTO_MAYOR")
                && !rolSolicitado.equals("FAMILIAR")
                && !rolSolicitado.equals("ACOMPANANTE")) {
            throw badRequest("El rol no está permitido para registro público");
        }
        validarPerfilCorrespondiente(dto, rolSolicitado);
        if (usuarioRepository.existsById(dni)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El DNI ya está registrado");
        }
        if (usuarioRepository.existsByCorreoElectronico(correo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo electrónico ya está registrado");
        }

        // Utiliza el catálogo de roles existente: no crea roles ni permite elevar privilegios.
        Rol rol = rolRepository.findAll().stream()
                .filter(r -> r.getNombre() != null
                        && r.getNombre().trim().toUpperCase(Locale.ROOT)
                                .replace(' ', '_').equals(rolSolicitado))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El rol solicitado no está configurado en la tabla roles"));

        Usuario usuario = new Usuario();
        usuario.setDni(dni);
        usuario.setNombres(nombres);
        usuario.setApellidos(apellidos);
        usuario.setCorreoElectronico(correo);
        usuario.setContrasenaHash(passwordEncoder.encode(dto.contrasena()));
        usuario.setTelefono(telefono);
        usuario.setRol(rol);
        usuario.setEstaActivo(true);
        usuarioRepository.save(usuario);

        switch (rolSolicitado) {
            case "ADULTO_MAYOR" -> crearAdultoMayor(usuario, dto.adultoMayor());
            case "FAMILIAR" -> crearFamiliar(usuario, dto.familiar());
            case "ACOMPANANTE" -> crearAcompanante(usuario, dto.acompanante());
            default -> throw badRequest("Rol no permitido");
        }
        return UsuarioDTO.desdeEntidad(usuario);
    }

    private void validarPerfilCorrespondiente(RegistroUsuarioDTO dto, String rol) {
        boolean valido = switch (rol) {
            case "ADULTO_MAYOR" -> dto.adultoMayor() != null
                    && dto.familiar() == null && dto.acompanante() == null;
            case "FAMILIAR" -> dto.familiar() != null
                    && dto.adultoMayor() == null && dto.acompanante() == null;
            case "ACOMPANANTE" -> dto.acompanante() != null
                    && dto.adultoMayor() == null && dto.familiar() == null;
            default -> false;
        };
        if (!valido) throw badRequest("Debe enviar únicamente el perfil correspondiente al rol");
    }

    private void crearAdultoMayor(Usuario usuario, RegistroUsuarioDTO.AdultoMayor datos) {
        if (datos.fechaNacimiento() == null || datos.fechaNacimiento().isAfter(LocalDate.now())) {
            throw badRequest("Debe indicar una fecha de nacimiento válida");
        }
        PerfilAdultoMayor perfil = new PerfilAdultoMayor();
        perfil.setUsuario(usuario);
        perfil.setFechaNacimiento(datos.fechaNacimiento());
        perfil.setDireccionResidencia(opcional(datos.direccionResidencia()));
        perfil.setContactoEmergencia(requerido(datos.contactoEmergencia(), "contacto de emergencia"));
        perfil.setTelefonoEmergencia(requerido(datos.telefonoEmergencia(), "teléfono de emergencia"));
        if (perfil.getContactoEmergencia().length() > 100 || perfil.getTelefonoEmergencia().length() > 20) {
            throw badRequest("Los datos del contacto de emergencia son demasiado largos");
        }
        perfil.setObservacionesMedicas(opcional(datos.observacionesMedicas()));
        adultoMayorRepository.save(perfil);
    }

    private void crearFamiliar(Usuario usuario, RegistroUsuarioDTO.Familiar datos) {
        PerfilFamiliar perfil = new PerfilFamiliar();
        perfil.setUsuario(usuario);
        if (datos.parentescoPrincipalId() != null) {
            perfil.setParentescoPrincipal(parentescoRepository.findById(datos.parentescoPrincipalId())
                    .orElseThrow(() -> badRequest("El parentesco indicado no existe")));
        }
        String telefono = opcional(datos.telefonoAlternativo());
        if (telefono != null && telefono.length() > 20) {
            throw badRequest("El teléfono alternativo es demasiado largo");
        }
        perfil.setTelefonoAlternativo(telefono);
        familiarRepository.save(perfil);
    }

    private void crearAcompanante(Usuario usuario, RegistroUsuarioDTO.Acompanante datos) {
        BigDecimal tarifaBase = datos.tarifaBasePorHora();
        BigDecimal tarifaActual = datos.tarifaActualPorHora() != null
                ? datos.tarifaActualPorHora() : tarifaBase;
        validarTarifa(tarifaBase, "tarifa base");
        validarTarifa(tarifaActual, "tarifa actual");

        PerfilAcompanante perfil = new PerfilAcompanante();
        perfil.setUsuario(usuario);
        perfil.setBiografia(opcional(datos.biografia()));
        perfil.setNivelAcademico(opcional(datos.nivelAcademico()));
        if (perfil.getNivelAcademico() != null && perfil.getNivelAcademico().length() > 100) {
            throw badRequest("El nivel académico es demasiado largo");
        }
        perfil.setTarifaBasePorHora(tarifaBase);
        perfil.setTarifaActualPorHora(tarifaActual);
        String ruc = opcional(datos.institucionRuc());
        if (ruc != null) {
            if (!ruc.matches("\\d{11}")) throw badRequest("El RUC debe contener 11 dígitos");
            perfil.setInstitucionAliada(institucionRepository.findById(ruc)
                    .orElseThrow(() -> badRequest("La institución aliada indicada no existe")));
        }
        acompananteRepository.save(perfil);
    }

    private void validarTarifa(BigDecimal tarifa, String nombre) {
        if (tarifa == null || tarifa.signum() < 0 || tarifa.scale() > 2 || tarifa.precision() > 10) {
            throw badRequest("Debe indicar una " + nombre + " válida (máximo 2 decimales)");
        }
    }

    private String requerido(String valor, String campo) {
        if (valor == null || valor.isBlank()) throw badRequest("El campo " + campo + " es obligatorio");
        return valor.trim();
    }

    private String opcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private ResponseStatusException badRequest(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
}
