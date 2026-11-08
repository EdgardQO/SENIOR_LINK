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

@Service
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PerfilAcompananteRepository acompananteRepository;
    private final ServicioRepository servicioRepository;
    private final DescuentoRepository descuentoRepository;

    public ReservaService(ReservaRepository reservaRepository,
                          UsuarioRepository usuarioRepository,
                          PerfilAcompananteRepository acompananteRepository,
                          ServicioRepository servicioRepository,
                          DescuentoRepository descuentoRepository) {
        this.reservaRepository = reservaRepository;
        this.usuarioRepository = usuarioRepository;
        this.acompananteRepository = acompananteRepository;
        this.servicioRepository = servicioRepository;
        this.descuentoRepository = descuentoRepository;
    }

    @Transactional
    public Reserva crearReserva(CrearReservaDTO dto) {
        Usuario cliente = usuarioRepository.findById(dto.clienteDni())
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado con DNI: " + dto.clienteDni()));

        Usuario usuarioAcomp = usuarioRepository.findById(dto.acompananteDni())
                .orElseThrow(() -> new IllegalArgumentException("Acompañante no encontrado con DNI: " + dto.acompananteDni()));

        PerfilAcompanante perfilAcomp = acompananteRepository.findById(dto.acompananteDni())
                .orElseThrow(() -> new IllegalArgumentException("Perfil del acompañante no configurado"));

        if (dto.servicios() == null || dto.servicios().isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos un servicio");
        }

        // Sumar horas de todos los servicios solicitados
        BigDecimal totalHoras = BigDecimal.ZERO;
        for (var item : dto.servicios()) {
            totalHoras = totalHoras.add(item.horas());
        }

        // Se usa la tarifa por hora del perfil del acompañante (afectada por sus reseñas)
        BigDecimal tarifaPorHora = perfilAcomp.getTarifaActualPorHora();
        BigDecimal subtotal = tarifaPorHora.multiply(totalHoras).setScale(2, RoundingMode.HALF_UP);

        // Descuentos
        BigDecimal montoDescuento = BigDecimal.ZERO;
        Descuento descuentoAplicado = null;
        if (dto.codigoCupon() != null && !dto.codigoCupon().isBlank()) {
            descuentoAplicado = descuentoRepository.findCuponValido(dto.codigoCupon(), OffsetDateTime.now())
                    .orElseThrow(() -> new IllegalArgumentException("Cupón inválido o expirado"));

            if (subtotal.compareTo(descuentoAplicado.getMontoMinimoOrden()) >= 0) {
                if (descuentoAplicado.getTipo() == TipoDescuento.PORCENTAJE) {
                    montoDescuento = subtotal.multiply(descuentoAplicado.getValor())
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    if (descuentoAplicado.getMontoMaximoDescuento() != null &&
                            montoDescuento.compareTo(descuentoAplicado.getMontoMaximoDescuento()) > 0) {
                        montoDescuento = descuentoAplicado.getMontoMaximoDescuento();
                    }
                } else {
                    montoDescuento = descuentoAplicado.getValor();
                }
                descuentoAplicado.setCantidadUsada(descuentoAplicado.getCantidadUsada() + 1);
            }
        }

        BigDecimal comisionPlataforma = subtotal.multiply(new BigDecimal("0.15")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal montoTotal = subtotal.subtract(montoDescuento).setScale(2, RoundingMode.HALF_UP);

        Reserva reserva = new Reserva();
        reserva.setCliente(cliente);
        reserva.setAcompanante(usuarioAcomp);
        reserva.setDescuento(descuentoAplicado);
        reserva.setFechaHoraInicio(dto.fechaHoraInicio());
        reserva.setFechaHoraFin(dto.fechaHoraFin());
        reserva.setTotalHoras(totalHoras);
        reserva.setTarifaPorHora(tarifaPorHora);
        reserva.setSubtotal(subtotal);
        reserva.setMontoDescuento(montoDescuento);
        reserva.setComisionPlataforma(comisionPlataforma);
        reserva.setMontoTotal(montoTotal);
        reserva.setDireccionEncuentro(dto.direccionEncuentro());
        reserva.setIndicacionesAdicionales(dto.indicacionesAdicionales());
        reserva.setEstadoActual(EstadoReserva.PENDIENTE_PAGO);

        for (var item : dto.servicios()) {
            Servicio serv = servicioRepository.findById(item.servicioId())
                    .orElseThrow(() -> new IllegalArgumentException("Servicio no encontrado con ID: " + item.servicioId()));
            reserva.agregarServicio(serv, item.horas());
        }

        return reservaRepository.save(reserva);
    }
}