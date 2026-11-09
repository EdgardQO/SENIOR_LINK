package com.seniorlink.api.service;

import com.seniorlink.api.dto.*;
import com.seniorlink.api.model.entity.*;
import com.seniorlink.api.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PerfilAdultoMayorRepository adultoMayorRepository;
    private final PerfilFamiliarRepository familiarRepository;
    private final PerfilAcompananteRepository acompananteRepository;
    private final PerfilSoporteRepository soporteRepository;
    private final PerfilAdminRepository adminRepository;
    private final ParentescoRepository parentescoRepository;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            PerfilAdultoMayorRepository adultoMayorRepository,
            PerfilFamiliarRepository familiarRepository,
            PerfilAcompananteRepository acompananteRepository,
            PerfilSoporteRepository soporteRepository,
            PerfilAdminRepository adminRepository,
            ParentescoRepository parentescoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.adultoMayorRepository = adultoMayorRepository;
        this.familiarRepository = familiarRepository;
        this.acompananteRepository = acompananteRepository;
        this.soporteRepository = soporteRepository;
        this.adminRepository = adminRepository;
        this.parentescoRepository = parentescoRepository;
    }

    @Transactional(readOnly = true)
    public TotalUsuariosResponseDTO obtenerUsuariosConTotal() {
        List<UsuarioDTO> dtos = usuarioRepository.findAll().stream()
                .map(UsuarioDTO::desdeEntidad)
                .toList();
        return new TotalUsuariosResponseDTO(dtos.size(), dtos);
    }

    @Transactional(readOnly = true)
    public List<UsuarioDTO> listarTodosLosUsuarios() {
        return usuarioRepository.findAll().stream()
                .map(UsuarioDTO::desdeEntidad)
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioDetalleDTO obtenerDetalle(String dni) {
        Usuario usuario = buscarUsuario(dni);
        return construirDetalle(usuario);
    }

    @Transactional
    public UsuarioDetalleDTO actualizar(String dni, ActualizarUsuarioDTO solicitud) {
        Usuario usuario = buscarUsuario(dni);

        if (solicitud.correoElectronico() != null) {
            String correo = solicitud.correoElectronico().trim().toLowerCase();
            usuarioRepository.findByCorreoElectronico(correo)
                    .filter(otro -> !otro.getDni().equals(dni))
                    .ifPresent(otro -> {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "El correo electrónico ya está registrado por otro usuario");
                    });
            usuario.setCorreoElectronico(correo);
        }
        if (solicitud.nombres() != null) usuario.setNombres(solicitud.nombres().trim());
        if (solicitud.apellidos() != null) usuario.setApellidos(solicitud.apellidos().trim());
        if (solicitud.telefono() != null) usuario.setTelefono(solicitud.telefono().trim());
        if (solicitud.estaActivo() != null) usuario.setEstaActivo(solicitud.estaActivo());

        // Los perfiles son opcionales y solo se modifican si existen para ese usuario.
        if (solicitud.adultoMayor() != null) {
            PerfilAdultoMayor p = adultoMayorRepository.findById(dni)
                    .orElseThrow(() -> perfilNoExiste("adulto mayor"));
            var d = solicitud.adultoMayor();
            if (d.fechaNacimiento() != null) p.setFechaNacimiento(d.fechaNacimiento());
            if (d.direccionResidencia() != null) p.setDireccionResidencia(d.direccionResidencia());
            if (d.contactoEmergencia() != null) p.setContactoEmergencia(d.contactoEmergencia());
            if (d.telefonoEmergencia() != null) p.setTelefonoEmergencia(d.telefonoEmergencia());
            if (d.observacionesMedicas() != null) p.setObservacionesMedicas(d.observacionesMedicas());
        }

        if (solicitud.familiar() != null) {
            PerfilFamiliar p = familiarRepository.findById(dni)
                    .orElseThrow(() -> perfilNoExiste("familiar"));
            var d = solicitud.familiar();
            if (d.parentescoPrincipalId() != null) {
                p.setParentescoPrincipal(parentescoRepository.findById(d.parentescoPrincipalId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "El parentesco indicado no existe")));
            }
            if (d.telefonoAlternativo() != null) p.setTelefonoAlternativo(d.telefonoAlternativo());
        }

        if (solicitud.acompanante() != null) {
            PerfilAcompanante p = acompananteRepository.findById(dni)
                    .orElseThrow(() -> perfilNoExiste("acompañante"));
            var d = solicitud.acompanante();
            if (d.biografia() != null) p.setBiografia(d.biografia());
            if (d.ciclo() != null) {
                if (d.ciclo() < 1 || d.ciclo() > 20) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "El ciclo debe ser un número entre 1 y 20");
                }
                p.setCiclo(d.ciclo());
            }
            if (d.carrera() != null) {
                String carrera = d.carrera().trim();
                if (carrera.isEmpty() || carrera.length() > 150) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "La carrera debe tener entre 1 y 150 caracteres");
                }
                p.setCarrera(carrera);
            }
            if (d.tarifaBasePorHora() != null) p.setTarifaBasePorHora(d.tarifaBasePorHora());
            if (d.tarifaActualPorHora() != null) p.setTarifaActualPorHora(d.tarifaActualPorHora());
            // promedioCalificacion y totalCalificaciones son métricas del sistema: no editables aquí.
        }

        if (solicitud.soporte() != null) {
            PerfilSoporte p = soporteRepository.findById(dni)
                    .orElseThrow(() -> perfilNoExiste("soporte"));
            var d = solicitud.soporte();
            if (d.turno() != null) p.setTurno(d.turno());
            if (d.extensionTelefonica() != null) p.setExtensionTelefonica(d.extensionTelefonica());
            // casosAtendidos es un contador operativo y no se modifica desde este endpoint.
        }

        if (solicitud.admin() != null) {
            PerfilAdmin p = adminRepository.findById(dni)
                    .orElseThrow(() -> perfilNoExiste("administrador"));
            var d = solicitud.admin();
            if (d.areaODepartamento() != null) p.setAreaODepartamento(d.areaODepartamento());
            if (d.nivelAcceso() != null) p.setNivelAcceso(d.nivelAcceso());
        }

        usuario.setActualizadoEn(OffsetDateTime.now());
        usuarioRepository.save(usuario);
        return construirDetalle(usuario);
    }

    private Usuario buscarUsuario(String dni) {
        return usuarioRepository.findById(dni)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe un usuario con DNI " + dni));
    }

    private ResponseStatusException perfilNoExiste(String perfil) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "El usuario no tiene un perfil de tipo " + perfil);
    }

    private UsuarioDetalleDTO construirDetalle(Usuario u) {
        String dni = u.getDni();
        var adulto = adultoMayorRepository.findById(dni).map(p ->
                new UsuarioDetalleDTO.AdultoMayor(
                        p.getFechaNacimiento(), p.getDireccionResidencia(),
                        p.getContactoEmergencia(), p.getTelefonoEmergencia(),
                        p.getObservacionesMedicas())).orElse(null);
        var familiar = familiarRepository.findById(dni).map(p ->
                new UsuarioDetalleDTO.Familiar(
                        p.getParentescoPrincipal() != null ? p.getParentescoPrincipal().getId() : null,
                        p.getParentescoPrincipal() != null ? p.getParentescoPrincipal().getNombre() : null,
                        p.getTelefonoAlternativo())).orElse(null);
        var acompanante = acompananteRepository.findById(dni).map(p ->
                new UsuarioDetalleDTO.Acompanante(
                        p.getBiografia(), p.getCiclo(), p.getCarrera(), p.getTarifaBasePorHora(),
                        p.getTarifaActualPorHora(), p.getPromedioCalificacion(),
                        p.getTotalCalificaciones(),
                        p.getInstitucionAliada() == null ? null : InstitucionAliadaDTO.desdeEntidad(p.getInstitucionAliada()))).orElse(null);
        var soporte = soporteRepository.findById(dni).map(p ->
                new UsuarioDetalleDTO.Soporte(
                        p.getTurno(), p.getExtensionTelefonica(), p.getCasosAtendidos())).orElse(null);
        var admin = adminRepository.findById(dni).map(p ->
                new UsuarioDetalleDTO.Admin(p.getAreaODepartamento(), p.getNivelAcceso())).orElse(null);
        return new UsuarioDetalleDTO(UsuarioDTO.desdeEntidad(u), adulto, familiar, acompanante, soporte, admin);
    }
}