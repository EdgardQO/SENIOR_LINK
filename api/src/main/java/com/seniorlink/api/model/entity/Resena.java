package com.seniorlink.api.model.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "resenas")
public class Resena {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "codigo_identificador", updatable = false, nullable = false)
    private UUID codigoIdentificador;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_codigo_id", unique = true, nullable = false)
    private Reserva reserva;

    @Column(name = "calificacion", nullable = false)
    private Short calificacion;

    @Column(name = "comentario", columnDefinition = "TEXT")
    private String comentario;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    public UUID getCodigoIdentificador() { return codigoIdentificador; }
    public void setCodigoIdentificador(UUID codigoIdentificador) { this.codigoIdentificador = codigoIdentificador; }
    public Reserva getReserva() { return reserva; }
    public void setReserva(Reserva reserva) { this.reserva = reserva; }
    public Short getCalificacion() { return calificacion; }
    public void setCalificacion(Short calificacion) { this.calificacion = calificacion; }
    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }
    public OffsetDateTime getCreadoEn() { return creadoEn; }
}