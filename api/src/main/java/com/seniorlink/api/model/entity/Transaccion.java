package com.seniorlink.api.model.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
@Entity @Getter @Setter
@Table(name="transacciones",schema="pasarela")
public class Transaccion {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="tarjeta_id",nullable=false) private Tarjeta tarjeta;
 @Column(nullable=false) private Long reservaId;
 @Column(nullable=false,unique=true) private UUID claveIdempotencia;
 @Column(nullable=false,precision=12,scale=2) private BigDecimal monto;
 @Column(nullable=false,length=20) private String estado;
 @Column(length=50) private String codigo;
 @Column(nullable=false) private OffsetDateTime creadoEn=OffsetDateTime.now();
}
