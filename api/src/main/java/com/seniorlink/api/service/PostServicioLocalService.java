package com.seniorlink.api.service;

import com.seniorlink.api.model.entity.*;
import com.seniorlink.api.model.enums.*;
import com.seniorlink.api.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.*;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@Transactional
public class PostServicioLocalService {
 private final UsuarioRepository usuarios;
 private final ReservaRepository reservas;
 private final ReservaLocalService motor;
 private final IncidenciaReservaRepository incidencias;
 private final LiquidacionAcompananteRepository liquidaciones;
 private final ValoracionRepository valoraciones;
 private final PerfilAcompananteRepository perfiles;
 private final VinculacionFamiliarAdultoRepository vinculos;
 private final PagoRepository pagos;
 public PostServicioLocalService(UsuarioRepository usuarios,ReservaRepository reservas,
  ReservaLocalService motor,IncidenciaReservaRepository incidencias,
  LiquidacionAcompananteRepository liquidaciones,ValoracionRepository valoraciones,
  PerfilAcompananteRepository perfiles,VinculacionFamiliarAdultoRepository vinculos,
  PagoRepository pagos){
  this.usuarios=usuarios;this.reservas=reservas;this.motor=motor;this.incidencias=incidencias;
  this.liquidaciones=liquidaciones;this.valoraciones=valoraciones;
  this.perfiles=perfiles;this.vinculos=vinculos;this.pagos=pagos;
 }
 public record IncidenciaVista(Long id,Long reservaId,String reportadoPor,
  String descripcion,String estado,String resolucion,OffsetDateTime creadoEn){}
 public record LiquidacionVista(Long id,Long reservaId,String acompananteDni,
  BigDecimal monto,String moneda,String estado,OffsetDateTime registradoEn){}
 public record ValoracionVista(Long id,Long reservaId,short puntuacion,String comentario){}
 private ResponseStatusException no(){return new ResponseStatusException(HttpStatus.FORBIDDEN,"Acceso denegado");}
 private ResponseStatusException bad(String m){return new ResponseStatusException(HttpStatus.BAD_REQUEST,m);}
 private Usuario usuario(String dni){return usuarios.findById(dni).orElseThrow(this::no);}
 private boolean esRol(Usuario u,String...roles){return Arrays.asList(roles).contains(u.getRol().getNombre());}
 private Reserva reserva(Long id){return reservas.bloquear(id)
  .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Reserva no encontrada"));}
 private IncidenciaVista vista(IncidenciaReserva i){
  return new IncidenciaVista(i.getId(),i.getReserva().getId(),i.getReportadoPor().getDni(),
   i.getDescripcion(),i.getEstado(),i.getResolucion(),i.getCreadoEn());
 }
 private LiquidacionVista vista(LiquidacionAcompanante l){
  return new LiquidacionVista(l.getId(),l.getReserva().getId(),l.getAcompanante().getDni(),
   l.getMonto(),l.getMoneda(),l.getEstado(),l.getRegistradoEn());
 }
 public IncidenciaVista reportar(String dni,Long reservaId,String descripcion){
  Usuario actor=usuario(dni);
  motor.obtener(dni,reservaId); // Verifica autorización con los permisos de vínculos.
  if(descripcion==null||descripcion.isBlank()||descripcion.length()>2000)throw bad("Describe la incidencia (máximo 2000 caracteres)");
  Reserva r=reserva(reservaId);
  if(!EnumSet.of(EstadoReserva.CONFIRMADA,EstadoReserva.EN_PROGRESO,
   EstadoReserva.PENDIENTE_CONFIRMACION,EstadoReserva.COMPLETADA,
   EstadoReserva.EN_DISPUTA).contains(r.getEstadoActual()))
   throw new ResponseStatusException(HttpStatus.CONFLICT,"La reserva no permite incidencias");
  IncidenciaReserva i=new IncidenciaReserva();
  i.setReserva(r);i.setReportadoPor(actor);i.setDescripcion(descripcion.trim());
  incidencias.saveAndFlush(i);
  if(r.getEstadoActual()!=EstadoReserva.EN_DISPUTA)
   motor.cambiar(r,EstadoReserva.EN_DISPUTA,"Incidencia reportada: requiere soporte",actor);
  return vista(i);
 }
 public List<IncidenciaVista> incidencias(String dni){
  Usuario u=usuario(dni);
  if(esRol(u,"ADMINISTRADOR","SOPORTE"))
   return incidencias.findAll().stream().map(this::vista).toList();
  return incidencias.findAll().stream().filter(i->{
   try{motor.obtener(dni,i.getReserva().getId());return true;}
   catch(ResponseStatusException ex){return false;}
  }).map(this::vista).toList();
 }
 public IncidenciaVista resolver(String dni,Long id,String resolucion){
  Usuario u=usuario(dni);if(!esRol(u,"ADMINISTRADOR","SOPORTE"))throw no();
  if(resolucion==null||resolucion.isBlank()||resolucion.length()>2000)throw bad("Indica una resolución");
  IncidenciaReserva i=incidencias.findById(id).orElseThrow(()->bad("Incidencia inexistente"));
  if(!"ABIERTA".equals(i.getEstado()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Incidencia ya cerrada");
  i.setEstado("RESUELTA");i.setResolucion(resolucion.trim());
  i.setResueltoPor(u);i.setResueltoEn(OffsetDateTime.now());
  return vista(incidencias.save(i));
 }
 public LiquidacionVista liquidar(String dni,Long id){
  Usuario admin=usuario(dni);if(!esRol(admin,"ADMINISTRADOR"))throw no();
  Reserva r=reserva(id);
  if(r.getEstadoActual()!=EstadoReserva.COMPLETADA)
   throw new ResponseStatusException(HttpStatus.CONFLICT,"Solo se liquidan servicios completados");
  if(incidencias.existsByReservaIdAndEstado(id,"ABIERTA"))
   throw new ResponseStatusException(HttpStatus.CONFLICT,"La reserva tiene incidencias abiertas");
  if(pagos.findByReservaId(id).filter(p->p.getMpEstado()==EstadoPasarelaPago.APROBADO).isEmpty())
   throw new ResponseStatusException(HttpStatus.CONFLICT,"No existe un pago aprobado");
  if(liquidaciones.findByReservaId(id).isPresent())
   throw new ResponseStatusException(HttpStatus.CONFLICT,"La liquidación ya fue registrada");
  LiquidacionAcompanante l=new LiquidacionAcompanante();
  l.setReserva(r);l.setAcompanante(r.getAcompanante());l.setRegistradoPor(admin);
  l.setMonto(r.getSubtotal().subtract(r.getMontoDescuento()).max(BigDecimal.ZERO));
  return vista(liquidaciones.save(l));
 }
 public List<LiquidacionVista> liquidaciones(String dni){
  Usuario actor=usuario(dni);
  if(!esRol(actor,"ADMINISTRADOR","ACOMPANANTE"))throw no();
  return liquidaciones.findAll().stream()
   .filter(l->esRol(actor,"ADMINISTRADOR")||l.getAcompanante().getDni().equals(dni))
   .map(this::vista).toList();
 }
 private boolean puedeValorar(String dni,Reserva r){
  if(dni.equals(r.getCliente().getDni()))return true;
  return vinculos.findByFamiliarDni(dni).stream().anyMatch(v->
   "ACEPTADO".equals(v.getEstado())&&v.isPermiteGestionar()&&
    v.getAdultoMayor().getDni().equals(r.getCliente().getDni()));
 }
 public ValoracionVista valorar(String dni,Long id,short puntuacion,String comentario){
  Usuario actor=usuario(dni);
  Reserva r=reserva(id);
  if(!puedeValorar(dni,r))throw no();
  if(r.getEstadoActual()!=EstadoReserva.COMPLETADA)
   throw new ResponseStatusException(HttpStatus.CONFLICT,"Primero debe completarse el servicio");
  if(puntuacion<1||puntuacion>5||comentario!=null&&comentario.length()>1000)throw bad("Valoración inválida");
  if(valoraciones.findByReservaId(id).isPresent())
   throw new ResponseStatusException(HttpStatus.CONFLICT,"La reserva ya fue valorada");
  Valoracion v=new Valoracion(r,actor,r.getAcompanante(),puntuacion,comentario);
  valoraciones.saveAndFlush(v);
  PerfilAcompanante p=perfiles.findById(r.getAcompanante().getDni()).orElseThrow(()->bad("Perfil no encontrado"));
  int total=p.getTotalCalificaciones();
  BigDecimal nueva=p.getPromedioCalificacion().multiply(BigDecimal.valueOf(total))
   .add(BigDecimal.valueOf(puntuacion)).divide(BigDecimal.valueOf(total+1),2,RoundingMode.HALF_UP);
  p.setPromedioCalificacion(nueva);p.setTotalCalificaciones(total+1);perfiles.save(p);
  return new ValoracionVista(v.getId(),r.getId(),v.getPuntuacion(),v.getComentario());
 }
}
