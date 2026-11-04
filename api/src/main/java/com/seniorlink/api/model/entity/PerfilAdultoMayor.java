package com.seniorlink.api.model.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "perfiles_adulto_mayor")
public class PerfilAdultoMayor {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "codigo_identificador", updatable = false, nullable = false)
    private UUID codigoIdentificador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "familiar_codigo_id", nullable = false)
    private Usuario familiar;

    @Column(name = "nombres", nullable = false, length = 100)
    private String nombres;

    @Column(name = "apellidos", nullable = false, length = 100)
    private String apellidos;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Column(name = "observaciones_medicas", columnDefinition = "TEXT")
    private String observacionesMedicas;

    @Column(name = "contacto_emergencia", length = 100)
    private String contactoEmergencia;

    @Column(name = "telefono_emergencia", length = 20)
    private String telefonoEmergencia;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    // Getters y Setters
    public UUID getCodigoIdentificador() { return codigoIdentificador; }
    public void setCodigoIdentificador(UUID codigoIdentificador) { this.codigoIdentificador = codigoIdentificador; }
    public Usuario getFamiliar() { return familiar; }
    public void setFamiliar(Usuario familiar) { this.familiar = familiar; }
    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }
    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }
    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }
    public String getObservacionesMedicas() { return observacionesMedicas; }
    public void setObservacionesMedicas(String observacionesMedicas) { this.observacionesMedicas = observacionesMedicas; }
    public String getContactoEmergencia() { return contactoEmergencia; }
    public void setContactoEmergencia(String contactoEmergencia) { this.contactoEmergencia = contactoEmergencia; }
    public String getTelefonoEmergencia() { return telefonoEmergencia; }
    public void setTelefonoEmergencia(String telefonoEmergencia) { this.telefonoEmergencia = telefonoEmergencia; }
    public OffsetDateTime getCreadoEn() { return creadoEn; }
}