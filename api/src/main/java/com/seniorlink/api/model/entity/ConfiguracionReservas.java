package com.seniorlink.api.model.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "configuracion_reservas")
@Getter
@Setter
public class ConfiguracionReservas {
    @Id
    private Short id;
    @Column(name = "minutos_aceptacion", nullable = false)
    private Integer minutosAceptacion;
    @Column(name = "minutos_pago", nullable = false)
    private Integer minutosPago;
    @Column(name = "minutos_anticipacion", nullable = false)
    private Integer minutosAnticipacion;
    @Column(name = "minutos_duracion_minima", nullable = false)
    private Integer minutosDuracionMinima;
    @Column(name = "minutos_duracion_maxima", nullable = false)
    private Integer minutosDuracionMaxima;
    @Column(name = "minutos_inicio_anticipado", nullable = false)
    private Integer minutosInicioAnticipado;
    @Column(name = "horas_confirmacion_automatica", nullable = false)
    private Integer horasConfirmacionAutomatica;
    @Column(name = "tarifa_minima_hora", nullable = false, precision = 10, scale = 2)
    private BigDecimal tarifaMinimaHora;
    @Column(name = "tarifa_maxima_hora", nullable = false, precision = 10, scale = 2)
    private BigDecimal tarifaMaximaHora;
    @Column(name = "porcentaje_comision", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeComision;
}
