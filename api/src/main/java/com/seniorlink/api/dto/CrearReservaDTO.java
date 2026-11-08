package com.seniorlink.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record CrearReservaDTO(
        String clienteDni,
        String acompananteDni,
        List<ServicioHorasDTO> servicios,
        String codigoCupon,
        OffsetDateTime fechaHoraInicio,
        OffsetDateTime fechaHoraFin,
        String direccionEncuentro,
        String indicacionesAdicionales
) {
    public record ServicioHorasDTO(Long servicioId, BigDecimal horas) {}
}