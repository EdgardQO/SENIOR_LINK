package com.seniorlink.api.model.entity;

import com.seniorlink.api.model.enums.TipoDescuento;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "descuentos")
public class Descuento {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "codigo_identificador", updatable = false, nullable = false)
    private UUID codigoIdentificador;

    @Column(name = "codigo_cupon", unique = true, length = 50)
    private String codigoCupon;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoDescuento tipo;

    @Column(name = "valor", nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "monto_minimo_orden", precision = 10, scale = 2)
    private BigDecimal montoMinimoOrden = BigDecimal.ZERO;

    @Column(name = "monto_maximo_descuento", precision = 10, scale = 2)
    private BigDecimal montoMaximoDescuento;

    @Column(name = "limite_usos")
    private Integer limiteUsos;

    @Column(name = "cantidad_usada", nullable = false)
    private Integer cantidadUsada = 0;

    @Column(name = "valido_desde", nullable = false)
    private OffsetDateTime validoDesde;

    @Column(name = "valido_hasta", nullable = false)
    private OffsetDateTime validoHasta;

    @Column(name = "esta_activo", nullable = false)
    private Boolean estaActivo = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    // Getters y Setters
    public UUID getCodigoIdentificador() { return codigoIdentificador; }
    public void setCodigoIdentificador(UUID codigoIdentificador) { this.codigoIdentificador = codigoIdentificador; }
    public String getCodigoCupon() { return codigoCupon; }
    public void setCodigoCupon(String codigoCupon) { this.codigoCupon = codigoCupon; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public TipoDescuento getTipo() { return tipo; }
    public void setTipo(TipoDescuento tipo) { this.tipo = tipo; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public BigDecimal getMontoMinimoOrden() { return montoMinimoOrden; }
    public void setMontoMinimoOrden(BigDecimal montoMinimoOrden) { this.montoMinimoOrden = montoMinimoOrden; }
    public BigDecimal getMontoMaximoDescuento() { return montoMaximoDescuento; }
    public void setMontoMaximoDescuento(BigDecimal montoMaximoDescuento) { this.montoMaximoDescuento = montoMaximoDescuento; }
    public Integer getLimiteUsos() { return limiteUsos; }
    public void setLimiteUsos(Integer limiteUsos) { this.limiteUsos = limiteUsos; }
    public Integer getCantidadUsada() { return cantidadUsada; }
    public void setCantidadUsada(Integer cantidadUsada) { this.cantidadUsada = cantidadUsada; }
    public OffsetDateTime getValidoDesde() { return validoDesde; }
    public void setValidoDesde(OffsetDateTime validoDesde) { this.validoDesde = validoDesde; }
    public OffsetDateTime getValidoHasta() { return validoHasta; }
    public void setValidoHasta(OffsetDateTime validoHasta) { this.validoHasta = validoHasta; }
    public Boolean getEstaActivo() { return estaActivo; }
    public void setEstaActivo(Boolean estaActivo) { this.estaActivo = estaActivo; }
    public OffsetDateTime getCreadoEn() { return creadoEn; }
}