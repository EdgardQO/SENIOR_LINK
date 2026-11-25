package com.seniorlink.api.model.entity;

import com.seniorlink.api.model.enums.EstadoReserva;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reservas")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_dni", nullable = false)
    private Usuario cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acompanante_dni", nullable = false)
    private Usuario acompanante;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="solicitante_dni")
    private Usuario solicitante;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="adulto_mayor_dni")
    private Usuario adultoMayor;
    @Column(name="vence_aceptacion_en") private OffsetDateTime venceAceptacionEn;
    @Column(name="aceptado_en") private OffsetDateTime aceptadoEn;
    @Column(name="vence_pago_en") private OffsetDateTime vencePagoEn;
    @Column(name="inicio_real_en") private OffsetDateTime inicioRealEn;
    @Column(name="fin_real_en") private OffsetDateTime finRealEn;
    @Column(name="confirmado_en") private OffsetDateTime confirmadoEn;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "descuento_id")
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

    @OneToMany(mappedBy = "reserva", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReservaServicio> servicios = new ArrayList<>();

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn = OffsetDateTime.now();

    public void agregarServicio(Servicio servicio, BigDecimal horas) {
        ReservaServicio item = new ReservaServicio();
        item.setReserva(this);
        item.setServicio(servicio);
        item.setHoras(horas);
        this.servicios.add(item);
    }

    public Usuario getSolicitante(){return solicitante;}
    public void setSolicitante(Usuario v){solicitante=v;}
    public Usuario getAdultoMayor(){return adultoMayor;}
    public void setAdultoMayor(Usuario v){adultoMayor=v;}
    public OffsetDateTime getVenceAceptacionEn(){return venceAceptacionEn;}
    public void setVenceAceptacionEn(OffsetDateTime v){venceAceptacionEn=v;}
    public OffsetDateTime getAceptadoEn(){return aceptadoEn;}
    public void setAceptadoEn(OffsetDateTime v){aceptadoEn=v;}
    public OffsetDateTime getVencePagoEn(){return vencePagoEn;}
    public void setVencePagoEn(OffsetDateTime v){vencePagoEn=v;}
    public OffsetDateTime getInicioRealEn(){return inicioRealEn;}
    public void setInicioRealEn(OffsetDateTime v){inicioRealEn=v;}
    public OffsetDateTime getFinRealEn(){return finRealEn;}
    public void setFinRealEn(OffsetDateTime v){finRealEn=v;}
    public OffsetDateTime getConfirmadoEn(){return confirmadoEn;}
    public void setConfirmadoEn(OffsetDateTime v){confirmadoEn=v;}
    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Usuario getCliente() { return cliente; }
    public void setCliente(Usuario cliente) { this.cliente = cliente; }
    public Usuario getAcompanante() { return acompanante; }
    public void setAcompanante(Usuario acompanante) { this.acompanante = acompanante; }
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
    public List<ReservaServicio> getServicios() { return servicios; }
    public void setServicios(List<ReservaServicio> servicios) { this.servicios = servicios; }
    public OffsetDateTime getCreadoEn() { return creadoEn; }
    public OffsetDateTime getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(OffsetDateTime actualizadoEn) { this.actualizadoEn = actualizadoEn; }
}