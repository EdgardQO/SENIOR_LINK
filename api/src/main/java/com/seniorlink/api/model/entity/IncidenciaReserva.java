package com.seniorlink.api.model.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
@Entity @Getter @Setter
@Table(name="incidencias_reserva")
public class IncidenciaReserva {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="reserva_id",nullable=false) private Reserva reserva;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="reportado_por_dni",nullable=false) private Usuario reportadoPor;
 @Column(nullable=false,columnDefinition="TEXT") private String descripcion;
 @Column(nullable=false,length=20) private String estado="ABIERTA";
 @Column(columnDefinition="TEXT") private String resolucion;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="resuelto_por_dni") private Usuario resueltoPor;
 @Column(nullable=false,updatable=false) private OffsetDateTime creadoEn=OffsetDateTime.now();
 private OffsetDateTime resueltoEn;
}
