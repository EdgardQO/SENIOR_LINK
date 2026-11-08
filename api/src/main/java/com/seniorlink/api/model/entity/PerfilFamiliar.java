package com.seniorlink.api.model.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "perfiles_familiar")
public class PerfilFamiliar {

    @Id
    @Column(name = "usuario_dni", length = 8)
    private String usuarioDni;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "usuario_dni")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parentesco_principal_id")
    private Parentesco parentescoPrincipal;

    @Column(name = "telefono_alternativo", length = 20)
    private String telefonoAlternativo;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    public String getUsuarioDni() { return usuarioDni; }
    public void setUsuarioDni(String usuarioDni) { this.usuarioDni = usuarioDni; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public Parentesco getParentescoPrincipal() { return parentescoPrincipal; }
    public void setParentescoPrincipal(Parentesco parentescoPrincipal) { this.parentescoPrincipal = parentescoPrincipal; }
    public String getTelefonoAlternativo() { return telefonoAlternativo; }
    public void setTelefonoAlternativo(String telefonoAlternativo) { this.telefonoAlternativo = telefonoAlternativo; }
    public OffsetDateTime getCreadoEn() { return creadoEn; }
}