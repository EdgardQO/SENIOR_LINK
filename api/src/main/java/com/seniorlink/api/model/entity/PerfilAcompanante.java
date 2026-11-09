package com.seniorlink.api.model.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "perfiles_acompanante")
public class PerfilAcompanante {

    @Id
    @Column(name = "usuario_dni", length = 8)
    private String usuarioDni;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "usuario_dni")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institucion_ruc", referencedColumnName = "ruc")
    private InstitucionAliada institucionAliada;

    @Column(name = "biografia", columnDefinition = "TEXT")
    private String biografia;

    @Column(name = "nivel_academico", length = 100)
    private String nivelAcademico;

    @Column(name = "tarifa_base_por_hora", nullable = false, precision = 10, scale = 2)
    private BigDecimal tarifaBasePorHora;

    @Column(name = "tarifa_actual_por_hora", nullable = false, precision = 10, scale = 2)
    private BigDecimal tarifaActualPorHora;

    @Column(name = "promedio_calificacion", nullable = false, precision = 3, scale = 2)
    private BigDecimal promedioCalificacion = new BigDecimal("5.00");

    @Column(name = "total_calificaciones", nullable = false)
    private Integer totalCalificaciones = 0;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    public String getUsuarioDni() { return usuarioDni; }
    public void setUsuarioDni(String usuarioDni) { this.usuarioDni = usuarioDni; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public String getBiografia() { return biografia; }
    public void setBiografia(String biografia) { this.biografia = biografia; }
    public String getNivelAcademico() { return nivelAcademico; }
    public void setNivelAcademico(String nivelAcademico) { this.nivelAcademico = nivelAcademico; }
    public BigDecimal getTarifaBasePorHora() { return tarifaBasePorHora; }
    public void setTarifaBasePorHora(BigDecimal tarifaBasePorHora) { this.tarifaBasePorHora = tarifaBasePorHora; }
    public BigDecimal getTarifaActualPorHora() { return tarifaActualPorHora; }
    public void setTarifaActualPorHora(BigDecimal tarifaActualPorHora) { this.tarifaActualPorHora = tarifaActualPorHora; }
    public BigDecimal getPromedioCalificacion() { return promedioCalificacion; }
    public void setPromedioCalificacion(BigDecimal promedioCalificacion) { this.promedioCalificacion = promedioCalificacion; }
    public Integer getTotalCalificaciones() { return totalCalificaciones; }
    public void setTotalCalificaciones(Integer totalCalificaciones) { this.totalCalificaciones = totalCalificaciones; }
    public InstitucionAliada getInstitucionAliada() { return institucionAliada; }
    public void setInstitucionAliada(InstitucionAliada institucionAliada) { this.institucionAliada = institucionAliada; }
    public OffsetDateTime getCreadoEn() { return creadoEn; }
}