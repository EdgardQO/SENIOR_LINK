package com.seniorlink.api.model.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "valoraciones")
public class Valoracion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_id", unique = true, nullable = false)
    private Reserva reserva;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_dni", nullable = false)
    private Usuario cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acompanante_dni", nullable = false)
    private Usuario acompanante;

    @Column(name = "puntuacion", nullable = false)
    private Short puntuacion; // Escala del 1 al 5

    @Column(name = "comentario", columnDefinition = "TEXT")
    private String comentario;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    public Valoracion() {
    }

    public Valoracion(Reserva reserva, Usuario cliente, Usuario acompanante, Short puntuacion, String comentario) {
        this.reserva = reserva;
        this.cliente = cliente;
        this.acompanante = acompanante;
        this.puntuacion = puntuacion;
        this.comentario = comentario;
        this.creadoEn = OffsetDateTime.now();
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Reserva getReserva() {
        return reserva;
    }

    public void setReserva(Reserva reserva) {
        this.reserva = reserva;
    }

    public Usuario getCliente() {
        return cliente;
    }

    public void setCliente(Usuario cliente) {
        this.cliente = cliente;
    }

    public Usuario getAcompanante() {
        return acompanante;
    }

    public void setAcompanante(Usuario acompanante) {
        this.acompanante = acompanante;
    }

    public Short getPuntuacion() {
        return puntuacion;
    }

    public void setPuntuacion(Short puntuacion) {
        this.puntuacion = puntuacion;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(OffsetDateTime creadoEn) {
        this.creadoEn = creadoEn;
    }
}