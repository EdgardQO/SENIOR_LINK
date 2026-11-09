package com.seniorlink.api.model.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "instituciones_aliadas")
public class InstitucionAliada {

    @Id
    @Column(name = "ruc", length = 11, nullable = false)
    private String ruc;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "detalles_convenio", columnDefinition = "TEXT")
    private String detallesConvenio;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    public String getRuc() { return ruc; }
    public void setRuc(String ruc) { this.ruc = ruc; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDetallesConvenio() { return detallesConvenio; }
    public void setDetallesConvenio(String detallesConvenio) { this.detallesConvenio = detallesConvenio; }
    public OffsetDateTime getCreadoEn() { return creadoEn; }
}