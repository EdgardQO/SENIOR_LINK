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
        for (EstadoReserva estado : values()) {
            if (estado.valor.equalsIgnoreCase(valor)) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Estado de reserva inválido: " + valor);
    }
}