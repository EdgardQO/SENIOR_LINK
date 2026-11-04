package com.seniorlink.api.model.entity;

import com.seniorlink.api.model.enums.EstadoReserva;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "reservas")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "codigo_identificador", updatable = false, nullable = false)
    private UUID codigoIdentificador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "familiar_codigo_id", nullable = false)
    private Usuario familiar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acompanante_codigo_id", nullable = false)
    private Usuario acompanante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adulto_mayor_codigo_id", nullable = false)
    private PerfilAdultoMayor adultoMayor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servicio_codigo_id", nullable = false)
    private Servicio servicio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "descuento_codigo_id")
    private Descuento descuento;

    @Column(name = "fecha_hora_inicio", nullable = false)
    private OffsetDateTime fechaHoraInicio;

    @Column(name = "fecha_hora_fin", nullable = false)
    private OffsetDateTime fechaHoraFin;

    @Column(name = "total_horas", nullable = false, precision = 4, scale = 2)
    private BigDecimal totalHoras;

    @Column(name = "tarifa_por_hora", nullable = false, precision = 10, scale = 2)
    private BigDecimal tarifaPorHora;

    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "monto_descuento", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoDescuento = BigDecimal.ZERO;

    @Column(name = "comision_plataforma", nullable = false, precision = 10, scale = 2)
    private BigDecimal comisionPlataforma = BigDecimal.ZERO;

    @Column(name = "monto_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_actual", nullable = false)
    private EstadoReserva estadoActual = EstadoReserva.PENDIENTE_PAGO;

    @Column(name = "direccion_encuentro", nullable = false, columnDefinition = "TEXT")
    private String direccionEncuentro;

    @Column(name = "indicaciones_adicionales", columnDefinition = "TEXT")
    private String indicacionesAdicionales;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn = OffsetDateTime.now();

    // Getters y Setters
    public UUID getCodigoIdentificador() { return codigoIdentificador; }
    public void setCodigoIdentificador(UUID codigoIdentificador) { this.codigoIdentificador = codigoIdentificador; }
    public Usuario getFamiliar() { return familiar; }
    public void setFamiliar(Usuario familiar) { this.familiar = familiar; }
    public Usuario getAcompanante() { return acompanante; }
    public void setAcompanante(Usuario acompanante) { this.acompanante = acompanante; }
    public PerfilAdultoMayor getAdultoMayor() { return adultoMayor; }
    public void setAdultoMayor(PerfilAdultoMayor adultoMayor) { this.adultoMayor = adultoMayor; }
    public Servicio getServicio() { return servicio; }
    public void setServicio(Servicio servicio) { this.servicio = servicio; }
    public Descuento getDescuento() { return descuento; }
    public void setDescuento(Descuento descuento) { this.descuento = descuento; }
    public OffsetDateTime getFechaHoraInicio() { return fechaHoraInicio; }
    public void setFechaHoraInicio(OffsetDateTime fechaHoraInicio) { this.fechaHoraInicio = fechaHoraInicio; }
    public OffsetDateTime getFechaHoraFin() { return fechaHoraFin; }
    public void setFechaHoraFin(OffsetDateTime fechaHoraFin) { this.fechaHoraFin = fechaHoraFin; }
    public BigDecimal getTotalHoras() { return totalHoras; }
    public void setTotalHoras(BigDecimal totalHoras) { this.totalHoras = totalHoras; }
    public BigDecimal getTarifaPorHora() { return tarifaPorHora; }
    public void setTarifaPorHora(BigDecimal tarifaPorHora) { this.tarifaPorHora = tarifaPorHora; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getMontoDescuento() { return montoDescuento; }
    public void setMontoDescuento(BigDecimal montoDescuento) { this.montoDescuento = montoDescuento; }
    public BigDecimal getComisionPlataforma() { return comisionPlataforma; }
    public void setComisionPlataforma(BigDecimal comisionPlataforma) { this.comisionPlataforma = comisionPlataforma; }
    public BigDecimal getMontoTotal() { return montoTotal; }
    public void setMontoTotal(BigDecimal montoTotal) { this.montoTotal = montoTotal; }
    public EstadoReserva getEstadoActual() { return estadoActual; }
    public void setEstadoActual(EstadoReserva estadoActual) { this.estadoActual = estadoActual; }
    public String getDireccionEncuentro() { return direccionEncuentro; }
    public void setDireccionEncuentro(String direccionEncuentro) { this.direccionEncuentro = direccionEncuentro; }
    public String getIndicacionesAdicionales() { return indicacionesAdicionales; }
    public void setIndicacionesAdicionales(String indicacionesAdicionales) { this.indicacionesAdicionales = indicacionesAdicionales; }
    public OffsetDateTime getCreadoEn() { return creadoEn; }
    public OffsetDateTime getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(OffsetDateTime actualizadoEn) { this.actualizadoEn = actualizadoEn; }
}