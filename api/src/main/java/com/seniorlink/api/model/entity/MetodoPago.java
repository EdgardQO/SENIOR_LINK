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
@Table(name="metodos_pago")
public class MetodoPago {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="usuario_dni",nullable=false) private Usuario usuario;
 @Column(nullable=false,unique=true) private UUID token;
 @Column(nullable=false,length=4) private String ultimosCuatro;
 @Column(nullable=false,length=10) private String tipo;
 @Column(nullable=false) private boolean activo=true;
}
