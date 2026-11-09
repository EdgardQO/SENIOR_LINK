package com.seniorlink.api.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum TipoDescuento {
    PORCENTAJE("PORCENTAJE"),
    MONTO_FIJO("MONTO FIJO");

    private final String valor;

    TipoDescuento(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    public static TipoDescuento desdeValor(String valor) {
        if (valor == null) return null;
        for (TipoDescuento t : values()) {
            if (t.valor.equalsIgnoreCase(valor) || t.name().equalsIgnoreCase(valor)) {
                return t;
            }
        }
        throw new IllegalArgumentException("TipoDescuento no válido: " + valor);
    }
}