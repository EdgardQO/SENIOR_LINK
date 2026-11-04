package com.seniorlink.api.model.entity;

import com.seniorlink.api.model.enums.EstadoPasarelaPago;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "pagos")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "codigo_identificador", updatable = false, nullable = false)
    private UUID codigoIdentificador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_codigo_id", nullable = false)
    private Reserva reserva;

    @Column(name = "monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "moneda", nullable = false, length = 3)
    private String moneda = "PEN";

    @Column(name = "mp_preferencia_id", length = 150)
    private String mpPreferenciaId;

    @Column(name = "mp_pago_id", unique = true, length = 150)
    private String mpPagoId;

    @Column(name = "mp_estado", nullable = false)
    private EstadoPasarelaPago mpEstado = EstadoPasarelaPago.PENDIENTE;

    @Column(name = "mp_detalle_estado", length = 255)
    private String mpDetalleEstado;

    @Column(name = "mp_metodo_pago", length = 50)
    private String mpMetodoPago;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "webhook_datos_crudos", columnDefinition = "jsonb")
    private Map<String, Object> webhookDatosCrudos;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn = OffsetDateTime.now();

    // Getters y Setters
    public UUID getCodigoIdentificador() { return codigoIdentificador; }
    public void setCodigoIdentificador(UUID codigoIdentificador) { this.codigoIdentificador = codigoIdentificador; }
    public Reserva getReserva() { return reserva; }
    public void setReserva(Reserva reserva) { this.reserva = reserva; }
    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }
    public String getMoneda() { return moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda; }
    public String getMpPreferenciaId() { return mpPreferenciaId; }
    public void setMpPreferenciaId(String mpPreferenciaId) { this.mpPreferenciaId = mpPreferenciaId; }
    public String getMpPagoId() { return mpPagoId; }
    public void setMpPagoId(String mpPagoId) { this.mpPagoId = mpPagoId; }
    public EstadoPasarelaPago getMpEstado() { return mpEstado; }
    public void setMpEstado(EstadoPasarelaPago mpEstado) { this.mpEstado = mpEstado; }
    public String getMpDetalleEstado() { return mpDetalleEstado; }
    public void setMpDetalleEstado(String mpDetalleEstado) { this.mpDetalleEstado = mpDetalleEstado; }
    public String getMpMetodoPago() { return mpMetodoPago; }
    public void setMpMetodoPago(String mpMetodoPago) { this.mpMetodoPago = mpMetodoPago; }
    public Map<String, Object> getWebhookDatosCrudos() { return webhookDatosCrudos; }
    public void setWebhookDatosCrudos(Map<String, Object> webhookDatosCrudos) { this.webhookDatosCrudos = webhookDatosCrudos; }
    public OffsetDateTime getCreadoEn() { return creadoEn; }
    public OffsetDateTime getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(OffsetDateTime actualizadoEn) { this.actualizadoEn = actualizadoEn; }
}