package com.seniorlink.api.model.entity;

import com.seniorlink.api.model.enums.TipoNotificacion;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "notificaciones")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "codigo_identificador", updatable = false, nullable = false)
    private UUID codigoIdentificador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_codigo_id", nullable = false)
    private Usuario usuario;

    @Column(name = "titulo", nullable = false, length = 150)
    private String titulo;

    @Column(name = "mensaje", nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoNotificacion tipo = TipoNotificacion.SISTEMA;

    @Column(name = "referencia_codigo_id")
    private UUID referenciaCodigoId;

    @Column(name = "esta_leida", nullable = false)
    private Boolean estaLeida = false;

    @Column(name = "leida_en")
    private OffsetDateTime leidaEn;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    // Getters y Setters
    public UUID getCodigoIdentificador() { return codigoIdentificador; }
    public void setCodigoIdentificador(UUID codigoIdentificador) { this.codigoIdentificador = codigoIdentificador; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public TipoNotificacion getTipo() { return tipo; }
    public void setTipo(TipoNotificacion tipo) { this.tipo = tipo; }
    public UUID getReferenciaCodigoId() { return referenciaCodigoId; }
    public void setReferenciaCodigoId(UUID referenciaCodigoId) { this.referenciaCodigoId = referenciaCodigoId; }
    public Boolean getEstaLeida() { return estaLeida; }
    public void setEstaLeida(Boolean estaLeida) { this.estaLeida = estaLeida; }
    public OffsetDateTime getLeidaEn() { return leidaEn; }
    public void setLeidaEn(OffsetDateTime leidaEn) { this.leidaEn = leidaEn; }
    public OffsetDateTime getCreadoEn() { return creadoEn; }
}