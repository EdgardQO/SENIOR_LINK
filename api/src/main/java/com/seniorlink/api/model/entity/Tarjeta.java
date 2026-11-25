package com.seniorlink.api.model.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
@Getter
@Setter
@Entity
@Table(name="tarjetas", schema="pasarela")
@org.hibernate.annotations.Check(constraints="numero_tarjeta ~ '^DEMO-[0-9]{8}$' AND mes_vencimiento BETWEEN 1 AND 12 AND saldo >= 0 AND limite_credito >= 0 AND credito_utilizado BETWEEN 0 AND limite_credito AND ((tipo_tarjeta='DEBITO' AND limite_credito=0 AND credito_utilizado=0) OR (tipo_tarjeta='CREDITO' AND saldo=0))")
public class Tarjeta {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="titular_dni",nullable=false,length=8) private String titularDni;
 @Column(name="numero_tarjeta",nullable=false,unique=true,length=19) private String numeroTarjeta;
 @Column(nullable=false,length=150) private String nombreTitular;
 @Column(nullable=false) private Short mesVencimiento;
 @Column(nullable=false) private Short anioVencimiento;
 @com.fasterxml.jackson.annotation.JsonIgnore @Column(nullable=false) private String cvvHash;
 @Column(nullable=false,length=10) private String tipoTarjeta;
 @com.fasterxml.jackson.annotation.JsonIgnore @Column(nullable=false,precision=12,scale=2) private BigDecimal saldo=BigDecimal.ZERO;
 @com.fasterxml.jackson.annotation.JsonIgnore @Column(nullable=false,precision=12,scale=2) private BigDecimal limiteCredito=BigDecimal.ZERO;
 @com.fasterxml.jackson.annotation.JsonIgnore @Column(nullable=false,precision=12,scale=2) private BigDecimal creditoUtilizado=BigDecimal.ZERO;
 @Column(nullable=false,length=20) private String estado="ACTIVA";
 @Column(nullable=false,length=3) private String moneda="PEN";
 @Version private Long version;
}
