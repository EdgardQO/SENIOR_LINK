package com.seniorlink.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CrearReservaDTO(
        UUID familiarId,
        UUID acompananteId,
        UUID adultoMayorId,
        UUID servicioId,
        String codigoCupon,
        OffsetDateTime fechaHoraInicio,
        OffsetDateTime fechaHoraFin,
        String direccionEncuentro,
        String indicacionesAdicionales
) {}