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
@Table(name="bloqueos_disponibilidad")
@org.hibernate.annotations.Check(constraints="fecha_hora_fin > fecha_hora_inicio")
public class BloqueoDisponibilidad {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="acompanante_dni",nullable=false) private Usuario acompanante;
 @Column(nullable=false) private OffsetDateTime fechaHoraInicio;
 @Column(nullable=false) private OffsetDateTime fechaHoraFin;
 @Column(length=250) private String motivo;
}
