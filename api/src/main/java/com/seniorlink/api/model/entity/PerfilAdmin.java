package com.seniorlink.api.model.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "perfiles_admin")
public class PerfilAdmin {

    @Id
    @Column(name = "usuario_dni", length = 8)
    private String usuarioDni;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "usuario_dni")
    private Usuario usuario;

    @Column(name = "area_o_departamento", nullable = false, length = 100)
    private String areaODepartamento;

    @Column(name = "nivel_acceso", nullable = false)
    private Short nivelAcceso = 1;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    public PerfilAdmin() {
    }

    public PerfilAdmin(Usuario usuario, String areaODepartamento, Short nivelAcceso) {
        this.usuario = usuario;
        this.usuarioDni = usuario.getDni();
        this.areaODepartamento = areaODepartamento;
        this.nivelAcceso = (nivelAcceso != null) ? nivelAcceso : 1;
    }

    // Getters y Setters
    public String getUsuarioDni() {
        return usuarioDni;
    }

    public void setUsuarioDni(String usuarioDni) {
        this.usuarioDni = usuarioDni;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getAreaODepartamento() {
        return areaODepartamento;
    }

    public void setAreaODepartamento(String areaODepartamento) {
        this.areaODepartamento = areaODepartamento;
    }

    public Short getNivelAcceso() {
        return nivelAcceso;
    }

    public void setNivelAcceso(Short nivelAcceso) {
        this.nivelAcceso = nivelAcceso;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(OffsetDateTime creadoEn) {
        this.creadoEn = creadoEn;
    }
}