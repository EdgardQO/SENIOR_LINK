package com.seniorlink.api.model.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
@Entity @Getter @Setter
@Table(name="movimientos_tarjeta",schema="pasarela")
public class MovimientoTarjeta {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="tarjeta_id",nullable=false) private Tarjeta tarjeta;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="transaccion_id",nullable=false) private Transaccion transaccion;
 @Column(nullable=false,length=20) private String tipoMovimiento;
 @Column(nullable=false,precision=12,scale=2) private BigDecimal monto;
 @Column(nullable=false,precision=12,scale=2) private BigDecimal saldoAnterior;
 @Column(nullable=false,precision=12,scale=2) private BigDecimal saldoPosterior;
 @Column(nullable=false) private OffsetDateTime creadoEn=OffsetDateTime.now();
}
