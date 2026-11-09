package com.seniorlink.api.controller;

import com.seniorlink.api.dto.*;
import com.seniorlink.api.model.entity.*;
import com.seniorlink.api.model.enums.*;
import com.seniorlink.api.repository.*;
import com.seniorlink.api.service.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * Administración centralizada. Los usuarios se desactivan (no se borran físicamente)
 * para conservar relaciones, reservas, pagos e historial.
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminGestionController {
    private final UsuarioRepository usuarios;
    private final ReservaRepository reservas;
    private final InstitucionAliadaRepository instituciones;
    private final DescuentoRepository descuentos;
    private final HistorialEstadoReservaRepository historial;
    private final UsuarioService usuarioService;
    private final RegistroUsuarioService registro;
    private final PersonalInternoService personal;

    public AdminGestionController(UsuarioRepository usuarios, ReservaRepository reservas,
            InstitucionAliadaRepository instituciones, DescuentoRepository descuentos,
            HistorialEstadoReservaRepository historial, UsuarioService usuarioService,
            RegistroUsuarioService registro, PersonalInternoService personal) {
        this.usuarios = usuarios; this.reservas = reservas; this.instituciones = instituciones;
        this.descuentos = descuentos; this.historial = historial; this.usuarioService = usuarioService;
        this.registro = registro; this.personal = personal;
    }

    public record Resumen(long usuarios, long usuariosActivos, long acompanantes,
                          long adultosMayores, long familiares, long soporte, long administradores,
                          long reservas, long reservasConfirmadas, long reservasCompletadas,
                          long instituciones, long promocionesActivas, BigDecimal totalReservado) {}

    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
    public Resumen dashboard() {
        var all = usuarios.findAll();
        var bookings = reservas.findAll();
        var promos = descuentos.findAll();
        return new Resumen(all.size(), all.stream().filter(u -> Boolean.TRUE.equals(u.getEstaActivo())).count(),
            contar(all,"ACOMPANANTE"), contar(all,"ADULTO_MAYOR"), contar(all,"FAMILIAR"),
            contar(all,"SOPORTE"), contar(all,"ADMINISTRADOR"), bookings.size(),
            bookings.stream().filter(r -> r.getEstadoActual() == EstadoReserva.CONFIRMADA).count(),
            bookings.stream().filter(r -> r.getEstadoActual() == EstadoReserva.COMPLETADA).count(),
            instituciones.count(), promos.stream().filter(p -> Boolean.TRUE.equals(p.getEstaActivo())).count(),
            bookings.stream().filter(r -> r.getEstadoActual() != EstadoReserva.CANCELADA)
                .map(Reserva::getMontoTotal).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }
    private long contar(List<Usuario> all, String rol) {
        return all.stream().filter(u -> u.getRol() != null && rol.equals(u.getRol().getNombre())).count();
    }

    @GetMapping("/usuarios")
    @Transactional(readOnly = true)
    public List<UsuarioDTO> usuarios(@RequestParam(required = false) String rol) {
        return usuarioService.listarTodosLosUsuarios().stream()
            .filter(u -> rol == null || rol.isBlank() || rol.equalsIgnoreCase(u.rolNombre()))
            .toList();
    }
    @GetMapping("/usuarios/{dni}")
    public UsuarioDetalleDTO detalle(@PathVariable String dni) { return usuarioService.obtenerDetalle(dni); }

    @PostMapping("/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioDTO crearPublico(@RequestBody RegistroUsuarioDTO dto) {
        return registro.registrar(dto);
    }

    @PutMapping("/usuarios/{dni}")
    public UsuarioDetalleDTO actualizar(@PathVariable String dni, @RequestBody ActualizarUsuarioDTO dto,
                                         Authentication authentication) {
        if (dni.equals(authentication.getName()) && Boolean.FALSE.equals(dto.estaActivo()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes desactivar tu propia cuenta");
        return usuarioService.actualizar(dni, dto);
    }

    @DeleteMapping("/usuarios/{dni}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void desactivar(@PathVariable String dni, Authentication authentication) {
        if (dni.equals(authentication.getName()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes desactivar tu propia cuenta");
        Usuario u = usuarios.findById(dni).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        u.setEstaActivo(false);
        u.setActualizadoEn(OffsetDateTime.now());
        usuarios.save(u);
    }

    public record ReservaAdmin(Long id, String clienteDni, String clienteNombre,
            String acompananteDni, String acompananteNombre, OffsetDateTime fechaHoraInicio,
            OffsetDateTime fechaHoraFin, String estado, String direccionEncuentro,
            BigDecimal montoTotal, OffsetDateTime creadoEn) {
        public static ReservaAdmin of(Reserva r) {
            return new ReservaAdmin(r.getId(), r.getCliente().getDni(),
                r.getCliente().getNombres() + " " + r.getCliente().getApellidos(),
                r.getAcompanante().getDni(),
                r.getAcompanante().getNombres() + " " + r.getAcompanante().getApellidos(),
                r.getFechaHoraInicio(), r.getFechaHoraFin(), r.getEstadoActual().getValor(),
                r.getDireccionEncuentro(), r.getMontoTotal(), r.getCreadoEn());
        }
    }
    @GetMapping("/reservas")
    @Transactional(readOnly = true)
    public List<ReservaAdmin> reservas() {
        return reservas.findAll().stream().map(ReservaAdmin::of).toList();
    }
    @GetMapping("/reservas/{id}")
    @Transactional(readOnly = true)
    public ReservaAdmin reserva(@PathVariable Long id) { return ReservaAdmin.of(buscarReserva(id)); }
    private Reserva buscarReserva(Long id) {
        return reservas.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva no encontrada"));
    }
    public record CambiarEstado(String estado, String motivo) {}
    @PatchMapping("/reservas/{id}/estado")
    @Transactional
    public ReservaAdmin cambiarEstado(@PathVariable Long id, @RequestBody CambiarEstado dto,
                                      Authentication authentication) {
        Reserva r = buscarReserva(id);
        if (dto == null || dto.estado() == null) throw bad("Indica un estado");
        EstadoReserva estado;
        try { estado = EstadoReserva.desdeValor(dto.estado()); }
        catch (IllegalArgumentException ex) { throw bad("Estado de reserva inválido"); }
        if (r.getEstadoActual() == estado) return ReservaAdmin.of(r);
        if (r.getEstadoActual() == EstadoReserva.COMPLETADA || r.getEstadoActual() == EstadoReserva.CANCELADA)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede modificar una reserva finalizada");
        r.setEstadoActual(estado);
        r.setActualizadoEn(OffsetDateTime.now());
        HistorialEstadoReserva h = new HistorialEstadoReserva();
        h.setReserva(r);
        h.setEstado(estado);
        h.setMotivoODetalle(dto.motivo() == null ? "Cambio realizado desde administración" : dto.motivo().trim());
        h.setCambiadoPor(usuarios.findById(authentication.getName()).orElseThrow());
        reservas.save(r);
        historial.save(h);
        return ReservaAdmin.of(r);
    }

    public record InstitucionInput(String ruc, String nombre, String detallesConvenio) {}
    @GetMapping("/instituciones")
    @Transactional(readOnly = true)
    public List<InstitucionAliadaDTO> instituciones() {
        return instituciones.findAll().stream().map(InstitucionAliadaDTO::desdeEntidad).toList();
    }
    @PostMapping("/instituciones")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public InstitucionAliadaDTO crearInstitucion(@RequestBody InstitucionInput dto) {
        if (dto == null || dto.ruc() == null || !dto.ruc().matches("\\d{11}")) throw bad("El RUC debe tener 11 dígitos");
        if (instituciones.existsById(dto.ruc())) throw new ResponseStatusException(HttpStatus.CONFLICT, "RUC ya registrado");
        InstitucionAliada i = new InstitucionAliada();
        i.setRuc(dto.ruc()); aplicarInstitucion(i, dto);
        return InstitucionAliadaDTO.desdeEntidad(instituciones.save(i));
    }
    @PutMapping("/instituciones/{ruc}")
    @Transactional
    public InstitucionAliadaDTO editarInstitucion(@PathVariable String ruc, @RequestBody InstitucionInput dto) {
        InstitucionAliada i = instituciones.findById(ruc).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (dto == null || (dto.ruc() != null && !ruc.equals(dto.ruc()))) throw bad("No se permite modificar el RUC");
        aplicarInstitucion(i,dto);
        return InstitucionAliadaDTO.desdeEntidad(instituciones.save(i));
    }
    private void aplicarInstitucion(InstitucionAliada i, InstitucionInput dto) {
        if (dto.nombre() == null || dto.nombre().isBlank() || dto.nombre().trim().length() > 150) throw bad("Nombre inválido");
        i.setNombre(dto.nombre().trim());
        i.setDetallesConvenio(dto.detallesConvenio());
    }
    @DeleteMapping("/instituciones/{ruc}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void borrarInstitucion(@PathVariable String ruc) {
        if (!instituciones.existsById(ruc)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        // Una FK existente impedirá eliminar instituciones vinculadas a acompañantes.
        try { instituciones.deleteById(ruc); instituciones.flush(); }
        catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La institución tiene perfiles asociados");
        }
    }

    public record PromocionDTO(UUID id, String codigoCupon, String descripcion, String tipo,
        BigDecimal valor, BigDecimal montoMinimoOrden, BigDecimal montoMaximoDescuento,
        Integer limiteUsos, Integer cantidadUsada, OffsetDateTime validoDesde,
        OffsetDateTime validoHasta, Boolean estaActivo) {
        public static PromocionDTO of(Descuento d) {
            return new PromocionDTO(d.getCodigoIdentificador(), d.getCodigoCupon(),
                d.getDescripcion(), d.getTipo().getValor(), d.getValor(), d.getMontoMinimoOrden(),
                d.getMontoMaximoDescuento(), d.getLimiteUsos(), d.getCantidadUsada(),
                d.getValidoDesde(), d.getValidoHasta(), d.getEstaActivo());
        }
    }
    public record PromocionInput(String codigoCupon, String descripcion, String tipo,
        BigDecimal valor, BigDecimal montoMinimoOrden, BigDecimal montoMaximoDescuento,
        Integer limiteUsos, OffsetDateTime validoDesde, OffsetDateTime validoHasta, Boolean estaActivo) {}
    @GetMapping("/promociones")
    @Transactional(readOnly = true)
    public List<PromocionDTO> promociones() {
        return descuentos.findAll().stream().map(PromocionDTO::of).toList();
    }
    @PostMapping("/promociones")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public PromocionDTO crearPromocion(@RequestBody PromocionInput dto) {
        Descuento d = new Descuento(); aplicarPromocion(d,dto);
        return PromocionDTO.of(descuentos.save(d));
    }
    @PutMapping("/promociones/{id}")
    @Transactional
    public PromocionDTO editarPromocion(@PathVariable UUID id, @RequestBody PromocionInput dto) {
        Descuento d = descuentos.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        aplicarPromocion(d,dto);
        return PromocionDTO.of(descuentos.save(d));
    }
    private void aplicarPromocion(Descuento d, PromocionInput dto) {
        if (dto == null || dto.tipo() == null || dto.valor() == null ||
            dto.valor().signum() <= 0 || dto.valor().scale() > 2 ||
            dto.validoDesde() == null || dto.validoHasta() == null ||
            !dto.validoHasta().isAfter(dto.validoDesde())) throw bad("Datos de promoción inválidos");
        TipoDescuento tipo;
        try { tipo = TipoDescuento.desdeValor(dto.tipo()); }
        catch (IllegalArgumentException ex) { throw bad("Tipo de descuento inválido"); }
        if (tipo == TipoDescuento.PORCENTAJE && dto.valor().compareTo(new BigDecimal("100")) > 0)
            throw bad("El porcentaje no puede superar 100");
        if (dto.limiteUsos() != null && dto.limiteUsos() < d.getCantidadUsada())
            throw bad("El límite no puede ser menor que los usos realizados");
        if (dto.montoMinimoOrden() != null && dto.montoMinimoOrden().signum() < 0) throw bad("Monto mínimo inválido");
        if (dto.montoMaximoDescuento() != null && dto.montoMaximoDescuento().signum() < 0) throw bad("Tope inválido");
        String cupon = dto.codigoCupon() == null ? null : dto.codigoCupon().trim().toUpperCase(Locale.ROOT);
        if (cupon != null && cupon.length() > 50) throw bad("Cupón demasiado largo");
        d.setCodigoCupon(cupon == null || cupon.isEmpty() ? null : cupon);
        d.setDescripcion(dto.descripcion());
        d.setTipo(tipo); d.setValor(dto.valor());
        d.setMontoMinimoOrden(dto.montoMinimoOrden() == null ? BigDecimal.ZERO : dto.montoMinimoOrden());
        d.setMontoMaximoDescuento(dto.montoMaximoDescuento());
        d.setLimiteUsos(dto.limiteUsos());
        d.setValidoDesde(dto.validoDesde()); d.setValidoHasta(dto.validoHasta());
        d.setEstaActivo(dto.estaActivo() == null ? true : dto.estaActivo());
    }
    @DeleteMapping("/promociones/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void desactivarPromocion(@PathVariable UUID id) {
        Descuento d = descuentos.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        d.setEstaActivo(false);
        descuentos.save(d);
    }

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
