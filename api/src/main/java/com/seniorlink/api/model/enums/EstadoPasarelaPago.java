package com.seniorlink.api.model.enums;

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

    public String getValor() {
        return valor;
    }

    public static EstadoPasarelaPago desdeValor(String valor) {
        for (EstadoPasarelaPago estado : values()) {
            if (estado.valor.equalsIgnoreCase(valor)) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Estado de pasarela no válido: " + valor);
    }
}