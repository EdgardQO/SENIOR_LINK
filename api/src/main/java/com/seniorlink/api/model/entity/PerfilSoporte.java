package com.seniorlink.api.model.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "perfiles_soporte")
public class PerfilSoporte {

    @Id
    @Column(name = "usuario_dni", length = 8)
    private String usuarioDni;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "usuario_dni")
    private Usuario usuario;

    @Column(name = "turno", nullable = false, length = 50)
    private String turno; // Ej. 'Mañana', 'Tarde', 'Noche'

    @Column(name = "extension_telefonica", length = 20)
    private String extensionTelefonica;

    @Column(name = "casos_atendidos", nullable = false)
    private Integer casosAtendidos = 0;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    public PerfilSoporte() {
    }

    public PerfilSoporte(Usuario usuario, String turno, String extensionTelefonica) {
        this.usuario = usuario;
        this.usuarioDni = usuario.getDni();
        this.turno = turno;
        this.extensionTelefonica = extensionTelefonica;
        this.casosAtendidos = 0;
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

    public String getTurno() {
        return turno;
    }

    public void setTurno(String turno) {
        this.turno = turno;
    }

    public String getExtensionTelefonica() {
        return extensionTelefonica;
    }

    public void setExtensionTelefonica(String extensionTelefonica) {
        this.extensionTelefonica = extensionTelefonica;
    }

    public Integer getCasosAtendidos() {
        return casosAtendidos;
    }

    public void setCasosAtendidos(Integer casosAtendidos) {
        this.casosAtendidos = casosAtendidos;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(OffsetDateTime creadoEn) {
        this.creadoEn = creadoEn;
    }
}