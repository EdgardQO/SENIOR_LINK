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
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PerfilAcompananteRepository acompananteRepository;
    private final ServicioRepository servicioRepository;
    private final DescuentoRepository descuentoRepository;
    private final HistorialEstadoReservaRepository historialRepository;

    public ReservaService(ReservaRepository reservaRepository,
                          UsuarioRepository usuarioRepository,
                          PerfilAcompananteRepository acompananteRepository,
                          ServicioRepository servicioRepository,
                          DescuentoRepository descuentoRepository,
                          HistorialEstadoReservaRepository historialRepository) {
        this.reservaRepository = reservaRepository;
        this.usuarioRepository = usuarioRepository;
        this.acompananteRepository = acompananteRepository;
        this.servicioRepository = servicioRepository;
        this.descuentoRepository = descuentoRepository;
        this.historialRepository = historialRepository;
    }

    @Transactional
    public Reserva crearReserva(CrearReservaDTO dto) {
        Usuario cliente = usuarioRepository.findById(dto.clienteDni())
                .orElseThrow(() -> new IllegalArgumentException("Cliente con DNI " + dto.clienteDni() + " no encontrado"));

        Usuario acompanante = usuarioRepository.findById(dto.acompananteDni())
                .orElseThrow(() -> new IllegalArgumentException("Acompañante con DNI " + dto.acompananteDni() + " no encontrado"));

        PerfilAcompanante perfilAcomp = acompananteRepository.findById(dto.acompananteDni())
                .orElseThrow(() -> new IllegalArgumentException("Perfil del acompañante no encontrado"));

        if (dto.servicios() == null || dto.servicios().isEmpty()) {
            throw new IllegalArgumentException("La reserva debe incluir al menos un servicio");
        }

        BigDecimal tarifaHora = perfilAcomp.getTarifaActualPorHora();
        BigDecimal totalHoras = BigDecimal.ZERO;

        Reserva reserva = new Reserva();
        reserva.setCliente(cliente);
        reserva.setAcompanante(acompanante);
        reserva.setFechaHoraInicio(dto.fechaHoraInicio());
        reserva.setFechaHoraFin(dto.fechaHoraFin());
        reserva.setTarifaPorHora(tarifaHora);
        reserva.setDireccionEncuentro(dto.direccionEncuentro());
        reserva.setIndicacionesAdicionales(dto.indicacionesAdicionales());

        // Asignar los servicios calculando el total de horas
        for (CrearReservaDTO.ItemServicioDTO item : dto.servicios()) {
            Servicio serv = servicioRepository.findById(item.servicioId())
                    .orElseThrow(() -> new IllegalArgumentException("Servicio ID " + item.servicioId() + " no existe"));
            reserva.agregarServicio(serv, item.horas());
            totalHoras = totalHoras.add(item.horas());
        }

        reserva.setTotalHoras(totalHoras);
        BigDecimal subtotal = tarifaHora.multiply(totalHoras).setScale(2, RoundingMode.HALF_UP);
        reserva.setSubtotal(subtotal);

        // Aplicación del cupón si se envía
        BigDecimal descuentoMonto = BigDecimal.ZERO;
        if (dto.codigoCupon() != null && !dto.codigoCupon().isBlank()) {
            Descuento desc = descuentoRepository.findCuponValido(dto.codigoCupon(), OffsetDateTime.now())
                    .orElseThrow(() -> new IllegalArgumentException("Cupón no válido o expirado"));

            if (subtotal.compareTo(desc.getMontoMinimoOrden()) >= 0) {
                if (desc.getTipo() == TipoDescuento.PORCENTAJE) {
                    descuentoMonto = subtotal.multiply(desc.getValor()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                } else {
                    descuentoMonto = desc.getValor();
                }

                if (desc.getMontoMaximoDescuento() != null && descuentoMonto.compareTo(desc.getMontoMaximoDescuento()) > 0) {
                    descuentoMonto = desc.getMontoMaximoDescuento();
                }

                desc.setCantidadUsada(desc.getCantidadUsada() + 1);
                descuentoRepository.save(desc);
                reserva.setDescuento(desc);
            }
        }

        reserva.setMontoDescuento(descuentoMonto);

        // Comisión estándar de la plataforma (10%)
        BigDecimal comision = subtotal.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
        reserva.setComisionPlataforma(comision);

        BigDecimal montoTotal = subtotal.subtract(descuentoMonto).add(comision).setScale(2, RoundingMode.HALF_UP);
        reserva.setMontoTotal(montoTotal);

        return reservaRepository.save(reserva);
    }

    @Transactional(readOnly = true)
    public List<Reserva> listarTodas() {
        return reservaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Reserva obtenerPorId(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada con ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<HistorialEstadoReserva> obtenerHistorial(Long reservaId) {
        return historialRepository.findByReservaIdOrderByRegistradoEnDesc(reservaId);
    }

    @Transactional
    public Reserva actualizarEstado(Long reservaId, EstadoReserva nuevoEstado) {
        Reserva reserva = obtenerPorId(reservaId);
        reserva.setEstadoActual(nuevoEstado);
        reserva.setActualizadoEn(OffsetDateTime.now());
        return reservaRepository.save(reserva);
    }
}