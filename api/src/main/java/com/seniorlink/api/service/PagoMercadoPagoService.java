package com.seniorlink.api.service;

import com.seniorlink.api.model.entity.Pago;
import com.seniorlink.api.model.entity.Reserva;
import com.seniorlink.api.model.enums.EstadoPasarelaPago;
import com.seniorlink.api.model.enums.EstadoReserva;
import com.seniorlink.api.repository.PagoRepository;
import com.seniorlink.api.repository.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class PagoMercadoPagoService {

    private final PagoRepository pagoRepository;
    private final ReservaRepository reservaRepository;

    public PagoMercadoPagoService(PagoRepository pagoRepository, ReservaRepository reservaRepository) {
        this.pagoRepository = pagoRepository;
        this.reservaRepository = reservaRepository;
    }

    @Transactional
    public Pago procesarWebhook(String mpPagoId, String status, String statusDetail, String metodoPago, Map<String, Object> payloadCompleto) {
        Pago pago = pagoRepository.findByMpPagoId(mpPagoId).orElseGet(() -> {
            Pago nuevo = new Pago();
            nuevo.setMpPagoId(mpPagoId);
            return nuevo;
        });

        EstadoPasarelaPago nuevoEstado = mapearEstado(status);
        pago.setMpEstado(nuevoEstado);
        pago.setMpDetalleEstado(statusDetail);
        pago.setMpMetodoPago(metodoPago);
        pago.setWebhookDatosCrudos(payloadCompleto);
        pago.setActualizadoEn(OffsetDateTime.now());

        // Si el pago es aprobado, actualizar el estado de la reserva
        if (nuevoEstado == EstadoPasarelaPago.APROBADO && pago.getReserva() != null) {
            Reserva reserva = pago.getReserva();
            reserva.setEstadoActual(EstadoReserva.CONFIRMADA);
            reserva.setActualizadoEn(OffsetDateTime.now());
            reservaRepository.save(reserva);
        }

        return pagoRepository.save(pago);
    }

    private EstadoPasarelaPago mapearEstado(String mpStatus) {
        return switch (mpStatus.toLowerCase()) {
            case "approved" -> EstadoPasarelaPago.APROBADO;
            case "in_process" -> EstadoPasarelaPago.EN_PROCESO;
            case "rejected" -> EstadoPasarelaPago.RECHAZADO;
            case "refunded" -> EstadoPasarelaPago.REEMBOLSADO;
            case "cancelled" -> EstadoPasarelaPago.CANCELADO;
            default -> EstadoPasarelaPago.PENDIENTE;
        };
    }
}