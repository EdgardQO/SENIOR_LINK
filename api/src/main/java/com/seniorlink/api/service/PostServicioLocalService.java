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
  String descripcion,String estado,String resolucion,OffsetDateTime creadoEn,
  String reportanteNombre,String reportanteTelefono){}
 public record LiquidacionVista(Long id,Long reservaId,String acompananteDni,
  BigDecimal monto,String moneda,String estado,OffsetDateTime registradoEn){}
 public record ValoracionVista(Long id,Long reservaId,short puntuacion,String comentario){}
 public record ResenaPublica(short puntuacion,String comentario,OffsetDateTime creadoEn){}
 @Transactional(readOnly=true)
 public List<ResenaPublica> resenasAcompanante(String dni){
  Usuario acomp=usuarios.findById(dni).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Acompañante no disponible"));
  if(!Boolean.TRUE.equals(acomp.getEstaActivo())||!acomp.isFotoVerificada()||!perfiles.existsById(dni))
   throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Acompañante no disponible");
  return motor.opiniones(dni).stream()
   .map(v->new ResenaPublica(v.puntuacion(),v.comentario(),v.creadoEn())).toList();
 }

 private ResponseStatusException no(){return new ResponseStatusException(HttpStatus.FORBIDDEN,"Acceso denegado");}
 private ResponseStatusException bad(String m){return new ResponseStatusException(HttpStatus.BAD_REQUEST,m);}
 private Usuario usuario(String dni){Usuario u=usuarios.findById(dni).orElseThrow(this::no);if(!Boolean.TRUE.equals(u.getEstaActivo()))throw no();return u;}
 private boolean esRol(Usuario u,String...roles){return Arrays.asList(roles).contains(u.getRol().getNombre());}
 private Reserva reserva(Long id){return reservas.bloquear(id)
  .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Reserva no encontrada"));}
 private IncidenciaVista vista(IncidenciaReserva i){
  return vista(i,false);
 }
 private IncidenciaVista vista(IncidenciaReserva i,boolean contactoAutorizado){
  Usuario reportante=i.getReportadoPor();
  return new IncidenciaVista(i.getId(),i.getReserva().getId(),reportante.getDni(),
   i.getDescripcion(),i.getEstado(),i.getResolucion(),i.getCreadoEn(),
   contactoAutorizado?(reportante.getNombres()+" "+reportante.getApellidos()).trim():null,
   contactoAutorizado?reportante.getTelefono():null);
 }
 private LiquidacionVista vista(LiquidacionAcompanante l){
  return new LiquidacionVista(l.getId(),l.getReserva().getId(),l.getAcompanante().getDni(),
   l.getMonto(),l.getMoneda(),l.getEstado(),l.getRegistradoEn());
 }
 public IncidenciaVista reportar(String dni,Long reservaId,String descripcion){
  Usuario actor=usuario(dni);
  Reserva r=motor.bloquearParaAgenda(reservaId);
  motor.obtener(dni,reservaId); // Verifica autorización con los permisos de vínculos.
  if(descripcion==null||descripcion.isBlank()||descripcion.length()>2000)throw bad("Describe la incidencia (máximo 2000 caracteres)");
  // Una consulta de soporte puede referirse a cualquier estado, incluso cancelaciones o pagos pendientes.
  if(!esRol(actor,"ADULTO_MAYOR","FAMILIAR","ACOMPANANTE"))throw no();
  IncidenciaReserva i=new IncidenciaReserva();
  i.setReserva(r);i.setReportadoPor(actor);i.setDescripcion(descripcion.trim());
  incidencias.saveAndFlush(i);
  // Registrar una incidencia no cancela, bloquea ni modifica automáticamente la reserva.
  // Las decisiones operativas y financieras conservan su flujo independiente.
  return vista(i);
 }
 public List<IncidenciaVista> incidencias(String dni){
  Usuario u=usuario(dni);
  if(Boolean.TRUE.equals(u.getEstaActivo()) && esRol(u,"ADMINISTRADOR","SOPORTE"))
   return incidencias.findAll().stream().map(i->vista(i,true)).toList();
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
  return vista(incidencias.save(i),Boolean.TRUE.equals(u.getEstaActivo()));
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
  String adulto=r.getAdultoMayor()!=null?r.getAdultoMayor().getDni():r.getCliente().getDni();
  if(dni.equals(adulto))return true;
  return vinculos.findByFamiliarDni(dni).stream().anyMatch(v->
   "ACEPTADO".equals(v.getEstado())&&v.isPermiteGestionar()&&
    v.getAdultoMayor().getDni().equals(adulto));
 }
 public ValoracionVista valorar(String dni,Long id,short puntuacion,String comentario){
  Usuario actor=usuario(dni);
  Reserva r=motor.bloquearParaAgenda(id);
  if(!esRol(actor,"ADULTO_MAYOR","FAMILIAR")||!puedeValorar(dni,r))throw no();
  if(r.getEstadoActual()!=EstadoReserva.COMPLETADA)
   throw new ResponseStatusException(HttpStatus.CONFLICT,"Primero debe completarse el servicio");
  if(puntuacion<1||puntuacion>5||comentario!=null&&comentario.length()>1000)throw bad("Valoración inválida");
  var existente=valoraciones.findByReservaId(id);
  if(existente.isPresent()){
   Valoracion v=existente.get();
   return new ValoracionVista(v.getId(),r.getId(),v.getPuntuacion(),v.getComentario());
  }
  var historica=motor.opinionReserva(id);
  if(historica!=null)return new ValoracionVista(null,r.getId(),historica.puntuacion(),historica.comentario());
  Valoracion v=new Valoracion(r,actor,r.getAcompanante(),puntuacion,comentario);
  valoraciones.saveAndFlush(v);
  PerfilAcompanante p=perfiles.findById(r.getAcompanante().getDni()).orElseThrow(()->bad("Perfil no encontrado"));
  var opiniones=motor.opiniones(r.getAcompanante().getDni());
  int total=opiniones.size();
  BigDecimal nueva=total==0?BigDecimal.ZERO:BigDecimal.valueOf(opiniones.stream().mapToInt(ReservaLocalService.OpinionVista::puntuacion).sum())
   .divide(BigDecimal.valueOf(total),2,RoundingMode.HALF_UP);
  p.setPromedioCalificacion(nueva);p.setTotalCalificaciones(total);perfiles.save(p);
  return new ValoracionVista(v.getId(),r.getId(),v.getPuntuacion(),v.getComentario());
 }
}
