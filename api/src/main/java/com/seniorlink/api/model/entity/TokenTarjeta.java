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
@Table(name="tokens",schema="pasarela")
public class TokenTarjeta {
 @Id private UUID token;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="tarjeta_id",nullable=false) private Tarjeta tarjeta;
 @Column(nullable=false) private boolean activo=true;
}
