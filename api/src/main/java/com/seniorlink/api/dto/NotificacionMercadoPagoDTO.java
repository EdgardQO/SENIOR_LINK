package com.seniorlink.api.dto;

import java.util.Map;

public record NotificacionMercadoPagoDTO(
        String action,
        String api_version,
        Map<String, String> data,
        String date_created,
        Long id,
        Boolean live_mode,
        String type,
        String user_id
) {}