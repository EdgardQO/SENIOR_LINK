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
    private final ReservaRepository reservaRepository;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            PerfilAdultoMayorRepository adultoMayorRepository,
            PerfilFamiliarRepository familiarRepository,
            PerfilAcompananteRepository acompananteRepository,
            PerfilSoporteRepository soporteRepository,
            PerfilAdminRepository adminRepository,
            ParentescoRepository parentescoRepository, ReservaRepository reservaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.adultoMayorRepository = adultoMayorRepository;
        this.familiarRepository = familiarRepository;
        this.acompananteRepository = acompananteRepository;
        this.soporteRepository = soporteRepository;
        this.adminRepository = adminRepository;
        this.parentescoRepository = parentescoRepository;
        this.reservaRepository = reservaRepository;
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
        if (solicitud.telefono() != null) {
            String anterior = usuario.getTelefono();
            usuario.setTelefono(solicitud.telefono().trim());
            if (!java.util.Objects.equals(anterior, usuario.getTelefono())) revocarContacto(dni);
        }
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

    public static String normalizarTelefono(String raw) {
        if (raw == null) return null;
        String t = raw.trim();
        if (!t.matches("[+0-9 ()-]+") || t.indexOf('+', 1) >= 0) return null;
        String n = t.replaceAll("[^0-9]", "");
        if (n.matches("9[0-9]{8}")) return "51" + n;
        if (n.matches("519[0-9]{8}")) return n;
        if (t.startsWith("00") && n.startsWith("00")) {
            n = n.substring(2);
            if (n.startsWith("51")) return n.matches("519[0-9]{8}") ? n : null;
            return n.matches("[1-9][0-9]{7,14}") ? n : null;
        }
        if (t.startsWith("+") && !n.startsWith("51") && n.matches("[1-9][0-9]{7,14}")) return n;
        return null;
    }

    @Transactional
    public UsuarioDetalleDTO actualizarContactoPropio(String dni, String telefono, String telefonoAlternativo) {
        Usuario usuario = buscarUsuario(dni);
        if (!Boolean.TRUE.equals(usuario.getEstaActivo()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cuenta inactiva");
        if (normalizarTelefono(telefono) == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingresa un celular peruano de nueve dígitos o un número internacional válido");
        String valor = "+" + normalizarTelefono(telefono);
        if (!java.util.Objects.equals(usuario.getTelefono(), valor)) {
            usuario.setTelefono(valor);
            revocarContacto(dni);
        }
        if (telefonoAlternativo != null) {
            if (!"FAMILIAR".equals(usuario.getRol().getNombre()))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el familiar puede actualizar su teléfono alternativo");
            PerfilFamiliar p = familiarRepository.findById(dni).orElseThrow(() -> perfilNoExiste("familiar"));
            String alternativo = telefonoAlternativo.trim();
            if (!alternativo.isEmpty() && normalizarTelefono(alternativo) == null)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Teléfono alternativo inválido");
            p.setTelefonoAlternativo(alternativo.isEmpty() ? null : "+" + normalizarTelefono(alternativo));
        }
        usuario.setActualizadoEn(OffsetDateTime.now());
        usuarioRepository.save(usuario);
        return construirDetalle(usuario);
    }

    public record PerfilPropio(String nombres, String apellidos, String telefono,
            ActualizarUsuarioDTO.Acompanante acompanante, ActualizarUsuarioDTO.AdultoMayor adultoMayor,
            ActualizarUsuarioDTO.Familiar familiar, ActualizarUsuarioDTO.Soporte soporte) {}

    private String validarTexto(String valor, int max, boolean requerido) {
        if (valor == null) return null;
        String limpio = valor.trim();
        if ((requerido && limpio.isEmpty()) || limpio.length() > max)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Revisa los campos obligatorios y su longitud máxima");
        return limpio;
    }

    @Transactional(readOnly = true)
    public List<java.util.Map<String, Object>> parentescosPropios() {
        return parentescoRepository.findAll().stream()
            .sorted(java.util.Comparator.comparing(Parentesco::getNombre))
            .map(p -> java.util.Map.<String,Object>of("id", p.getId(), "nombre", p.getNombre())).toList();
    }

    @Transactional
    public UsuarioDetalleDTO actualizarPerfilPropio(String dni, PerfilPropio input) {
        Usuario u = buscarUsuario(dni);
        if (!Boolean.TRUE.equals(u.getEstaActivo()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cuenta inactiva");
        String rol = u.getRol().getNombre();
        if ((input.acompanante()!=null && !"ACOMPANANTE".equals(rol))
            || (input.adultoMayor()!=null && !"ADULTO_MAYOR".equals(rol))
            || (input.familiar()!=null && !"FAMILIAR".equals(rol))
            || (input.soporte()!=null && !"SOPORTE".equals(rol)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Perfil no autorizado");
        String telefono = input.telefono();
        if (telefono != null && !java.util.Objects.equals(telefono.trim(), u.getTelefono())) {
            if (normalizarTelefono(telefono)==null)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Teléfono personal inválido");
            telefono = "+" + normalizarTelefono(telefono);
        } else telefono = null;
        var a = input.acompanante();
        var m = input.adultoMayor();
        var f = input.familiar();
        var t = input.soporte();
        if (a != null && (a.tarifaBasePorHora()!=null || a.tarifaActualPorHora()!=null))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usa el módulo de tarifas");
        if (m != null && m.fechaNacimiento()!=null)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La fecha de nacimiento requiere revisión administrativa");
        if (t != null && t.turno()!=null)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El turno es administrado por la institución");
        String alternativo = f==null?null:f.telefonoAlternativo();
        if (alternativo!=null && !alternativo.isBlank()) {
            if (normalizarTelefono(alternativo)==null)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Teléfono alternativo inválido");
            alternativo="+"+normalizarTelefono(alternativo);
        }
        return actualizar(dni, new ActualizarUsuarioDTO(null,
            validarTexto(input.nombres(),100,true), validarTexto(input.apellidos(),100,true), telefono, null,
            m==null?null:new ActualizarUsuarioDTO.AdultoMayor(null, validarTexto(m.direccionResidencia(),1000,false),
                validarTexto(m.contactoEmergencia(),100,true), validarTexto(m.telefonoEmergencia(),20,true),
                validarTexto(m.observacionesMedicas(),3000,false)),
            f==null?null:new ActualizarUsuarioDTO.Familiar(f.parentescoPrincipalId(),alternativo),
            a==null?null:new ActualizarUsuarioDTO.Acompanante(validarTexto(a.biografia(),3000,false),a.ciclo(),
                validarTexto(a.carrera(),150,true),null,null),
            t==null?null:new ActualizarUsuarioDTO.Soporte(null,validarTexto(t.extensionTelefonica(),20,false)),null));
    }

    private void revocarContacto(String dni) {
        reservaRepository.revocarWhatsappAcompanante(dni);
        reservaRepository.revocarWhatsappSolicitante(dni);
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