package com.seniorlink.api.model.entity;

import com.seniorlink.api.model.enums.EstadoVerificacion;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "perfiles_acompanante")
public class PerfilAcompanante {

    @Id
    @Column(name = "usuario_codigo_id")
    private UUID usuarioCodigoId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "usuario_codigo_id")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institucion_codigo_id")
    private InstitucionAliada institucion;

    @Column(name = "nivel_academico", length = 100)
    private String nivelAcademico;

    @Column(name = "biografia", columnDefinition = "TEXT")
    private String biografia;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_verificacion", nullable = false)
    private EstadoVerificacion estadoVerificacion = EstadoVerificacion.PENDIENTE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validado_por_usuario_id")
    private Usuario validadoPor;

    @Column(name = "observaciones_validacion", columnDefinition = "TEXT")
    private String observacionesValidacion;

    @Column(name = "tarifa_por_hora_personalizada", precision = 10, scale = 2)
    private BigDecimal tarifaPorHoraPersonalizada;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    // Getters y Setters
    public UUID getUsuarioCodigoId() { return usuarioCodigoId; }
    public void setUsuarioCodigoId(UUID usuarioCodigoId) { this.usuarioCodigoId = usuarioCodigoId; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public InstitucionAliada getInstitucion() { return institucion; }
    public void setInstitucion(InstitucionAliada institucion) { this.institucion = institucion; }
    public String getNivelAcademico() { return nivelAcademico; }
    public void setNivelAcademico(String nivelAcademico) { this.nivelAcademico = nivelAcademico; }
    public String getBiografia() { return biografia; }
    public void setBiografia(String biografia) { this.biografia = biografia; }
    public EstadoVerificacion getEstadoVerificacion() { return estadoVerificacion; }
    public void setEstadoVerificacion(EstadoVerificacion estadoVerificacion) { this.estadoVerificacion = estadoVerificacion; }
    public Usuario getValidadoPor() { return validadoPor; }
    public void setValidadoPor(Usuario validadoPor) { this.validadoPor = validadoPor; }
    public String getObservacionesValidacion() { return observacionesValidacion; }
    public void setObservacionesValidacion(String observacionesValidacion) { this.observacionesValidacion = observacionesValidacion; }
    public BigDecimal getTarifaPorHoraPersonalizada() { return tarifaPorHoraPersonalizada; }
    public void setTarifaPorHoraPersonalizada(BigDecimal tarifaPorHoraPersonalizada) { this.tarifaPorHoraPersonalizada = tarifaPorHoraPersonalizada; }
    public OffsetDateTime getCreadoEn() { return creadoEn; }
}