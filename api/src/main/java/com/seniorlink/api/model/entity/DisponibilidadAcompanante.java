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
@Table(name="disponibilidad_acompanante")
@org.hibernate.annotations.Check(constraints="dia_semana BETWEEN 1 AND 7 AND hora_fin > hora_inicio")
public class DisponibilidadAcompanante {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="acompanante_dni",nullable=false) private Usuario acompanante;
 @Column(nullable=false) private Short diaSemana;
 @Column(nullable=false) private LocalTime horaInicio;
 @Column(nullable=false) private LocalTime horaFin;
 @Column(nullable=false) private boolean estaActivo=true;
}
