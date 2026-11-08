package com.seniorlink.api.model.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "perfiles_adulto_mayor")
public class PerfilAdultoMayor {

    @Id
    @Column(name = "usuario_dni", length = 8)
    private String usuarioDni;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "usuario_dni")
    private Usuario usuario;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Column(name = "direccion_residencia", columnDefinition = "TEXT")
    private String direccionResidencia;

    @Column(name = "contacto_emergencia", nullable = false, length = 100)
    private String contactoEmergencia;

    @Column(name = "telefono_emergencia", nullable = false, length = 20)
    private String telefonoEmergencia;

    @Column(name = "observaciones_medicas", columnDefinition = "TEXT")
    private String observacionesMedicas;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    public String getUsuarioDni() { return usuarioDni; }
    public void setUsuarioDni(String usuarioDni) { this.usuarioDni = usuarioDni; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }
    public String getDireccionResidencia() { return direccionResidencia; }
    public void setDireccionResidencia(String direccionResidencia) { this.direccionResidencia = direccionResidencia; }
    public String getContactoEmergencia() { return contactoEmergencia; }
    public void setContactoEmergencia(String contactoEmergencia) { this.contactoEmergencia = contactoEmergencia; }
    public String getTelefonoEmergencia() { return telefonoEmergencia; }
    public void setTelefonoEmergencia(String telefonoEmergencia) { this.telefonoEmergencia = telefonoEmergencia; }
    public String getObservacionesMedicas() { return observacionesMedicas; }
    public void setObservacionesMedicas(String observacionesMedicas) { this.observacionesMedicas = observacionesMedicas; }
    public OffsetDateTime getCreadoEn() { return creadoEn; }
}