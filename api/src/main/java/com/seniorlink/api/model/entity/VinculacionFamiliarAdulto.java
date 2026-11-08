package com.seniorlink.api.model.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "vinculaciones_familiar_adulto")
public class VinculacionFamiliarAdulto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "familiar_dni", nullable = false)
    private Usuario familiar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adulto_mayor_dni", nullable = false)
    private Usuario adultoMayor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parentesco_id", nullable = false)
    private Parentesco parentesco;

    @Column(name = "estado", nullable = false)
    private String estado = "PENDIENTE";

    @Column(name = "solicitado_en", nullable = false, updatable = false)
    private OffsetDateTime solicitadoEn = OffsetDateTime.now();

    @Column(name = "respondido_en")
    private OffsetDateTime respondidoEn;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Usuario getFamiliar() { return familiar; }
    public void setFamiliar(Usuario familiar) { this.familiar = familiar; }
    public Usuario getAdultoMayor() { return adultoMayor; }
    public void setAdultoMayor(Usuario adultoMayor) { this.adultoMayor = adultoMayor; }
    public Parentesco getParentesco() { return parentesco; }
    public void setParentesco(Parentesco parentesco) { this.parentesco = parentesco; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public OffsetDateTime getSolicitadoEn() { return solicitadoEn; }
    public OffsetDateTime getRespondidoEn() { return respondidoEn; }
    public void setRespondidoEn(OffsetDateTime respondidoEn) { this.respondidoEn = respondidoEn; }
}