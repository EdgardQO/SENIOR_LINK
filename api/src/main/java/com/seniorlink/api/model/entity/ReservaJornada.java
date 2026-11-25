package com.seniorlink.api.model.entity;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.OffsetDateTime;

@Entity
@Table(name="reservas_jornadas",indexes=@Index(name="idx_jornadas_reserva",columnList="reserva_id,fecha_hora_inicio"))
public class ReservaJornada {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @JsonIgnore @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="reserva_id",nullable=false) private Reserva reserva;
 @Column(name="fecha_hora_inicio",nullable=false) private OffsetDateTime fechaHoraInicio;
 @Column(name="fecha_hora_fin",nullable=false) private OffsetDateTime fechaHoraFin;
 @Column(nullable=false,length=32) private String estado="PROGRAMADA";
 @Column(name="inicio_real_en") private OffsetDateTime inicioRealEn;
 @Column(name="fin_real_en") private OffsetDateTime finRealEn;
 @Column(name="confirmado_en") private OffsetDateTime confirmadoEn;
 @Column(name="creado_en",nullable=false,updatable=false) private OffsetDateTime creadoEn=OffsetDateTime.now();
 @Column(name="actualizado_en",nullable=false) private OffsetDateTime actualizadoEn=OffsetDateTime.now();
 protected ReservaJornada(){}
 public ReservaJornada(Reserva r,OffsetDateTime i,OffsetDateTime f){reserva=r;fechaHoraInicio=i;fechaHoraFin=f;}
 public Long getId(){return id;}
 public OffsetDateTime getFechaHoraInicio(){return fechaHoraInicio;}
 public OffsetDateTime getFechaHoraFin(){return fechaHoraFin;}
 public String getEstado(){return estado;}
 public OffsetDateTime getInicioRealEn(){return inicioRealEn;}
 public OffsetDateTime getFinRealEn(){return finRealEn;}
 public OffsetDateTime getConfirmadoEn(){return confirmadoEn;}
 public void setEstado(String e){estado=e;actualizadoEn=OffsetDateTime.now();}
 public void iniciar(OffsetDateTime a){inicioRealEn=a;setEstado("EN_PROGRESO");}
 public void finalizar(OffsetDateTime a){finRealEn=a;setEstado("PENDIENTE_CONFIRMACION");}
 public void confirmar(OffsetDateTime a){confirmadoEn=a;setEstado("COMPLETADA");}
}
