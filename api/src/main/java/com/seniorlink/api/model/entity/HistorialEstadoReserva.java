package com.seniorlink.api.model.entity;

import com.seniorlink.api.model.enums.EstadoReserva;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "historial_estados_reserva")
public class HistorialEstadoReserva {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "identificador_historial", updatable = false, nullable = false)
    private UUID identificadorHistorial;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_codigo_id", nullable = false)
    private Reserva reserva;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoReserva estado;

    @Column(name = "motivo_o_detalle", columnDefinition = "TEXT")
    private String motivoODetalle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cambiado_por_usuario_id")
    private Usuario cambiadoPor;

    @Column(name = "registrado_en", nullable = false, updatable = false)
    private OffsetDateTime registradoEn = OffsetDateTime.now();

    public UUID getIdentificadorHistorial() { return identificadorHistorial; }
    public Reserva getReserva() { return reserva; }
    public void setReserva(Reserva reserva) { this.reserva = reserva; }
    public EstadoReserva getEstado() { return estado; }
    public void setEstado(EstadoReserva estado) { this.estado = estado; }
    public String getMotivoODetalle() { return motivoODetalle; }
    public void setMotivoODetalle(String motivoODetalle) { this.motivoODetalle = motivoODetalle; }
    public Usuario getCambiadoPor() { return cambiadoPor; }
    public void setCambiadoPor(Usuario cambiadoPor) { this.cambiadoPor = cambiadoPor; }
    public OffsetDateTime getRegistradoEn() { return registradoEn; }
}