package com.seniorlink.api.service;

import com.seniorlink.api.dto.CrearReservaDTO;
import com.seniorlink.api.model.entity.*;
import com.seniorlink.api.model.enums.EstadoReserva;
import com.seniorlink.api.model.enums.TipoDescuento;
import com.seniorlink.api.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final HistorialEstadoReservaRepository historialRepository;
    private final UsuarioRepository usuarioRepository;
    private final PerfilAdultoMayorRepository adultoMayorRepository;
    private final ServicioRepository servicioRepository;
    private final DescuentoRepository descuentoRepository;
    private final PerfilAcompananteRepository acompananteRepository;

    public ReservaService(ReservaRepository reservaRepository,
                          HistorialEstadoReservaRepository historialRepository,
                          UsuarioRepository usuarioRepository,
                          PerfilAdultoMayorRepository adultoMayorRepository,
                          ServicioRepository servicioRepository,
                          DescuentoRepository descuentoRepository,
                          PerfilAcompananteRepository acompananteRepository) {
        this.reservaRepository = reservaRepository;
        this.historialRepository = historialRepository;
        this.usuarioRepository = usuarioRepository;
        this.adultoMayorRepository = adultoMayorRepository;
        this.servicioRepository = servicioRepository;
        this.descuentoRepository = descuentoRepository;
        this.acompananteRepository = acompananteRepository;
    }

    @Transactional
    public Reserva crearReserva(CrearReservaDTO dto) {
        Usuario familiar = usuarioRepository.findById(dto.familiarId())
                .orElseThrow(() -> new IllegalArgumentException("Familiar no encontrado"));
        Usuario acompanante = usuarioRepository.findById(dto.acompananteId())
                .orElseThrow(() -> new IllegalArgumentException("Acompañante no encontrado"));
        PerfilAdultoMayor adulto = adultoMayorRepository.findById(dto.adultoMayorId())
                .orElseThrow(() -> new IllegalArgumentException("Perfil del adulto mayor no encontrado"));
        Servicio servicio = servicioRepository.findById(dto.servicioId())
                .orElseThrow(() -> new IllegalArgumentException("Servicio no encontrado"));

        PerfilAcompanante perfilAcomp = acompananteRepository.findById(dto.acompananteId())
                .orElseThrow(() -> new IllegalArgumentException("Perfil de acompañante no validado"));

        // Calcular duración en horas
        long minutos = Duration.between(dto.fechaHoraInicio(), dto.fechaHoraFin()).toMinutes();
        if (minutos <= 0) {
            throw new IllegalArgumentException("La fecha fin debe ser posterior a la fecha inicio");
        }
        BigDecimal totalHoras = BigDecimal.valueOf(minutos).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

        // Tarifa por hora: se toma la personalizada o la base del servicio
        BigDecimal tarifaHora = perfilAcomp.getTarifaPorHoraPersonalizada() != null
                ? perfilAcomp.getTarifaPorHoraPersonalizada()
                : servicio.getPrecioBasePorHora();

        BigDecimal subtotal = tarifaHora.multiply(totalHoras).setScale(2, RoundingMode.HALF_UP);
        BigDecimal descuentoMonto = BigDecimal.ZERO;
        Descuento descuentoAplicado = null;

        // Validar y aplicar descuento si existe
        if (dto.codigoCupon() != null && !dto.codigoCupon().isBlank()) {
            descuentoAplicado = descuentoRepository.findCuponValido(dto.codigoCupon(), OffsetDateTime.now())
                    .orElseThrow(() -> new IllegalArgumentException("Cupón inválido o expirado"));

            if (subtotal.compareTo(descuentoAplicado.getMontoMinimoOrden()) >= 0) {
                if (descuentoAplicado.getTipo() == TipoDescuento.PORCENTAJE) {
                    descuentoMonto = subtotal.multiply(descuentoAplicado.getValor())
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    if (descuentoAplicado.getMontoMaximoDescuento() != null &&
                            descuentoMonto.compareTo(descuentoAplicado.getMontoMaximoDescuento()) > 0) {
                        descuentoMonto = descuentoAplicado.getMontoMaximoDescuento();
                    }
                } else {
                    descuentoMonto = descuentoAplicado.getValor();
                }
                descuentoAplicado.setCantidadUsada(descuentoAplicado.getCantidadUsada() + 1);
            }
        }

        // Comisión de la plataforma SeniorLink (ejemplo: 15% del subtotal)
        BigDecimal comisionPlataforma = subtotal.multiply(BigDecimal.valueOf(0.15)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal montoTotal = subtotal.subtract(descuentoMonto).setScale(2, RoundingMode.HALF_UP);

        Reserva reserva = new Reserva();
        reserva.setFamiliar(familiar);
        reserva.setAcompanante(acompanante);
        reserva.setAdultoMayor(adulto);
        reserva.setServicio(servicio);
        reserva.setDescuento(descuentoAplicado);
        reserva.setFechaHoraInicio(dto.fechaHoraInicio());
        reserva.setFechaHoraFin(dto.fechaHoraFin());
        reserva.setTotalHoras(totalHoras);
        reserva.setTarifaPorHora(tarifaHora);
        reserva.setSubtotal(subtotal);
        reserva.setMontoDescuento(descuentoMonto);
        reserva.setComisionPlataforma(comisionPlataforma);
        reserva.setMontoTotal(montoTotal);
        reserva.setDireccionEncuentro(dto.direccionEncuentro());
        reserva.setIndicacionesAdicionales(dto.indicacionesAdicionales());
        reserva.setEstadoActual(EstadoReserva.PENDIENTE_PAGO);

        return reservaRepository.save(reserva);
    }

    @Transactional
    public Reserva actualizarEstado(UUID reservaId, EstadoReserva nuevoEstado) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));
        reserva.setEstadoActual(nuevoEstado);
        reserva.setActualizadoEn(OffsetDateTime.now());
        // El trigger en PostgreSQL insertará automáticamente en historial_estados_reserva
        return reservaRepository.save(reserva);
    }

    @Transactional(readOnly = true)
    public Reserva obtenerPorId(UUID reservaId) {
        return reservaRepository.findById(reservaId)
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));
    }

    @Transactional(readOnly = true)
    public List<HistorialEstadoReserva> obtenerHistorial(UUID reservaId) {
        return historialRepository.findByReservaCodigoIdentificadorOrderByRegistradoEnDesc(reservaId);
    }

    //Todas las reservas
    @Transactional(readOnly = true)
    public List<Reserva> listarTodas() {
        return reservaRepository.findAll();
    }
}