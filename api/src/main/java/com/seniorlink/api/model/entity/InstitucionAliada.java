package com.seniorlink.api.model.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "instituciones_aliadas")
public class InstitucionAliada {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "codigo_identificador", updatable = false, nullable = false)
    private UUID codigoIdentificador;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "detalles_convenio", columnDefinition = "TEXT")
    private String detallesConvenio;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    public UUID getCodigoIdentificador() { return codigoIdentificador; }
    public void setCodigoIdentificador(UUID codigoIdentificador) { this.codigoIdentificador = codigoIdentificador; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDetallesConvenio() { return detallesConvenio; }
    public void setDetallesConvenio(String detallesConvenio) { this.detallesConvenio = detallesConvenio; }
    public OffsetDateTime getCreadoEn() { return creadoEn; }
}