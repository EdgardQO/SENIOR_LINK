package com.seniorlink.api.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum EstadoPasarelaPago {
    PENDIENTE("pendiente"),
    APROBADO("aprobado"),
    EN_PROCESO("en proceso"),
    RECHAZADO("rechazado"),
    REEMBOLSADO("reembolsado"),
    CANCELADO("cancelado");

    private final String valor;

    EstadoPasarelaPago(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    public static EstadoPasarelaPago desdeValor(String valor) {
        if (valor == null) return null;
        for (EstadoPasarelaPago e : values()) {
            if (e.valor.equalsIgnoreCase(valor) || e.name().equalsIgnoreCase(valor)) {
                return e;
            }
        }
        throw new IllegalArgumentException("EstadoPasarelaPago no válido: " + valor);
    }
}