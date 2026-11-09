package com.seniorlink.api.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum EstadoReserva {
    PENDIENTE_PAGO("PENDIENTE PAGO"),
    CONFIRMADA("CONFIRMADA"),
    EN_PROGRESO("EN PROGRESO"),
    COMPLETADA("COMPLETADA"),
    CANCELADA("CANCELADA");

    private final String valor;

    EstadoReserva(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    public static EstadoReserva desdeValor(String valor) {
        if (valor == null) return null;
        for (EstadoReserva e : values()) {
            if (e.valor.equalsIgnoreCase(valor) || e.name().equalsIgnoreCase(valor)) {
                return e;
            }
        }
        throw new IllegalArgumentException("EstadoReserva no válido: " + valor);
    }
}