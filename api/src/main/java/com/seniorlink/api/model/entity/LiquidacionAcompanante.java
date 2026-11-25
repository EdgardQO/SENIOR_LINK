package com.seniorlink.api.model.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
@Entity @Getter @Setter
@Table(name="liquidaciones_acompanante")
public class LiquidacionAcompanante {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="reserva_id",unique=true,nullable=false) private Reserva reserva;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="acompanante_dni",nullable=false) private Usuario acompanante;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="registrado_por_dni",nullable=false) private Usuario registradoPor;
 @Column(nullable=false,precision=12,scale=2) private BigDecimal monto;
 @Column(nullable=false,length=3) private String moneda="PEN";
 @Column(nullable=false,length=20) private String estado="REGISTRADA";
 @Column(nullable=false) private OffsetDateTime registradoEn=OffsetDateTime.now();
}
